package me.jackles.resultnavigation

import android.app.Activity
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Parcelable
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentTransaction
import kotlinx.coroutines.CancellableContinuation
import kotlin.coroutines.resume

private const val PREFIX = "for.result.execute"
private const val HAS_RESULT ="$PREFIX.has.result"
private const val RESULT ="$PREFIX.result"
private const val DELAYED_RESULT ="$PREFIX.delayed.result"
private const val CALLER_KEY ="$PREFIX.caller"
private const val ACTION_KEY ="$PREFIX.action.key"
private const val SAVE_INSTANCE_KEY ="$PREFIX.action.save.instance"
private var uniqueValue = 0L

fun <T: Parcelable>launchForResult(cancellableContinuation: CancellableContinuation<ChoiceResult<T>>,
                                   activity: FragmentActivity,
                                   fragment: Fragment,
                                   fragmentManager: FragmentManager,
                                   fragmentTransaction: FragmentTransaction) {
    LaunchForResult().execute(cancellableContinuation,
        activity,
        fragment,
        fragmentManager,
        fragmentTransaction)
}

fun <T: Parcelable>launchForResult(cancellableContinuation: CancellableContinuation<ChoiceResult<T>>,
                                   activity: FragmentActivity,
                                   intent: Intent) {
    LaunchForResult().execute(cancellableContinuation,
        activity,
        intent)
}

fun endWithResult(activity: FragmentActivity,
                  result: Parcelable) {
    activity.intent.putExtra(HAS_RESULT, true)
    activity.sendBroadcast(Intent(activity.intent.getStringExtra(ACTION_KEY))
        .putExtra(RESULT, result))
    activity.finish()
}

fun endWithResult(fragment: Fragment,
                  result: Parcelable) {
    fragment.activity?.let { activity ->
        fragment.arguments
            ?.putBoolean(HAS_RESULT, true)
        fragment.arguments
            ?.putParcelable(RESULT, result)
//        activity.sendBroadcast(Intent(fragment.arguments?.getString(ACTION_KEY))
//            .putExtra(RESULT, result))
        fragment.fragmentManager?.popBackStackImmediate()
    }
}


private open class BroadcastReceiverActivityLifecycleCallbacks(internal val broadcastReceiver: BroadcastReceiver?,
                                                               internal val activityIntentKey: String,
                                                               internal val activityIntentValue: String,
                                                               internal val action: String,
                                                               internal val callerId: String,
                                                               internal val onDestroyedCallback: ((BroadcastReceiverActivityLifecycleCallbacks) -> Unit)? = null): Application.ActivityLifecycleCallbacks {
    override fun onActivityPaused(activity: Activity?) {}

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityDestroyed(activity: Activity) {
        onDestroyedCallback?.invoke(this)
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        if (activity.intent.getStringExtra(CALLER_KEY) == callerId) {
            outState.putString(CALLER_KEY, callerId)
        } else {
            outState.remove(CALLER_KEY)
        }
    }

    override fun onActivityStarted(activity: Activity) {
        broadcastReceiver?.let {
            if (activity.intent.getStringExtra(activityIntentKey) == activityIntentValue) {
                activity.registerReceiver(broadcastReceiver, IntentFilter(action))
            }
        }
    }

    override fun onActivityStopped(activity: Activity) {
        broadcastReceiver?.let {
            if (activity.intent.getStringExtra(activityIntentKey) == activityIntentValue) {
                activity.unregisterReceiver(broadcastReceiver)
            }
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {

    }
}

sealed class ChoiceResult<T: Parcelable>(open val data: T?) {
    data class OK<T: Parcelable>(override val data: T?): ChoiceResult<T>(data)
    data class Cancelled<T: Parcelable>(override val data: T? = null): ChoiceResult<T>(null)
}

class LaunchForResult {
    private fun <T : Parcelable> broadcastReceiverFactory(
        onResultRetrieved: (T) -> Unit
    ): BroadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                context.unregisterReceiver(this)

                val result = intent.getParcelableExtra<T>(RESULT)
                onResultRetrieved(result)
            }
        }

    private lateinit var broadcastReceiver: BroadcastReceiver

    private fun generateCallerKey() = CALLER_KEY + uniqueValue

    @Synchronized
    fun <T: Parcelable> execute(
        continuation: CancellableContinuation<ChoiceResult<T>>,
        callingActivity: FragmentActivity,
        intent: Intent
    ) {
        val action = PREFIX + uniqueValue
        val callerId = generateCallerKey()

        var result: T? = null

        broadcastReceiver = broadcastReceiverFactory { it: T->
            result = it
        }

        callingActivity.intent.putExtra(CALLER_KEY, callerId)
        callingActivity.application.registerActivityLifecycleCallbacks(object :
            BroadcastReceiverActivityLifecycleCallbacks(broadcastReceiver,
                activityIntentKey = ACTION_KEY,
                activityIntentValue = action,
                action = action,
                callerId = callerId) {
            private fun handleBundle(bundle: Bundle?, activity: Activity) {
                val cancelUnregisterCallbacks = object: BroadcastReceiverActivityLifecycleCallbacks(this.broadcastReceiver,
                    activityIntentKey = this.activityIntentKey,
                    activityIntentValue = this.activityIntentValue,
                    action = this.action,
                    callerId = this.callerId,
                    onDestroyedCallback = {
                        activity.application.unregisterActivityLifecycleCallbacks(it)
                        continuation.resume(ChoiceResult.Cancelled())
                    }){
                }
                if (bundle?.containsKey(CALLER_KEY) == true) {
                    result?.let {
                        continuation.resume(ChoiceResult.OK(it))
                    }?: kotlin.run {
                        activity.application.registerActivityLifecycleCallbacks(cancelUnregisterCallbacks)
                    }
                    activity.application.unregisterActivityLifecycleCallbacks(this)
                }
            }

            override fun onActivityResumed(activity: Activity) {
                handleBundle(activity.intent.extras,
                    activity)
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                handleBundle(savedInstanceState, activity)
            }
        })

        intent.putExtra(ACTION_KEY, action)

        callingActivity.startActivity(intent)

        uniqueValue++
    }

    @Synchronized
    fun <T: Parcelable> execute(
        continuation: CancellableContinuation<ChoiceResult<T>>,
        callingActivity: FragmentActivity,
        resultFragment: Fragment,
        fragmentManager: FragmentManager,
        fragmentTransaction: FragmentTransaction,
        _bundle: Bundle? = null
    ) {
        val bundle = _bundle ?: Bundle()

        val callerId = generateCallerKey()
        val action = PREFIX + uniqueValue
        callingActivity.intent.putExtra(CALLER_KEY, callerId)

        resultFragment.arguments = bundle

        val application = callingActivity.application

        var activityCallbacks: Application.ActivityLifecycleCallbacks? = null

//        broadcastReceiver = broadcastReceiverFactory { it : T ->
//            continuation.resume(it)
//        }

        val fragmentCallbacks = object:
            FragmentManager.FragmentLifecycleCallbacks() {
            var hasOnSaveInstanceStateBeenCalled = false
            override fun onFragmentCreated(fm: FragmentManager, f: Fragment, savedInstanceState: Bundle?) {
                super.onFragmentCreated(fm, f, savedInstanceState)
                hasOnSaveInstanceStateBeenCalled = false
                if (savedInstanceState != null && savedInstanceState.containsKey(ACTION_KEY)) {
                    f.arguments?.putString(ACTION_KEY,
                        savedInstanceState.getString(ACTION_KEY))
                }
            }

            override fun onFragmentSaveInstanceState(fm: FragmentManager, f: Fragment, outState: Bundle) {
                super.onFragmentSaveInstanceState(fm, f, outState)
                outState.putString(ACTION_KEY, action)
                hasOnSaveInstanceStateBeenCalled = true
            }

            override fun onFragmentDestroyed(fm: FragmentManager, f: Fragment) {
                fm.unregisterFragmentLifecycleCallbacks(this)
                if (!hasOnSaveInstanceStateBeenCalled) {
                    application.unregisterActivityLifecycleCallbacks(activityCallbacks)
                    if (f.arguments?.getBoolean(HAS_RESULT) == true) {
                        continuation.resume(ChoiceResult.OK(f.arguments!!.getParcelable(RESULT)))
                    } else {
                        continuation.resume(ChoiceResult.Cancelled())
                    }
                }
            }
        }

        activityCallbacks = object : BroadcastReceiverActivityLifecycleCallbacks(null,
            activityIntentKey = CALLER_KEY,
            activityIntentValue = callerId,
            action = action,
            callerId = callerId) {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (savedInstanceState != null) {
                    activity.intent.putExtra(CALLER_KEY, savedInstanceState.getString(CALLER_KEY))
                }
                activity as FragmentActivity
                activity.supportFragmentManager.registerFragmentLifecycleCallbacks(fragmentCallbacks,
                    false)
            }
        }

        fragmentManager.registerFragmentLifecycleCallbacks(fragmentCallbacks, false)

        callingActivity.application.registerActivityLifecycleCallbacks(activityCallbacks)
        bundle.putString(ACTION_KEY, action)

        fragmentTransaction.commitAllowingStateLoss()

        uniqueValue++
    }
}