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
import kotlin.coroutines.resume

private const val PREFIX = "for.result.execute"

const val HAS_RESULT ="$PREFIX.has.result"
const val RESULT ="$PREFIX.result"
const val ACTION_KEY ="$PREFIX.action.key"

private const val CALLER_KEY ="$PREFIX.caller"
private var uniqueValue = 0L

data class ActivityParams(
    internal val callingActivity: FragmentActivity,
    internal val intent: Intent
)

data class FragmentParams(
    internal val callingActivity: FragmentActivity,
    internal val fragment: Fragment,
    internal val fragmentManager: FragmentManager,
    internal val fragmentTransaction: FragmentTransaction
)

private open class StandardActivityLifecycleCallbacks(internal val broadcastReceiver: BroadcastReceiver?,
                                                      internal val activityIntentKey: String,
                                                      internal val activityIntentValue: String,
                                                      internal val action: String,
                                                      internal val callerId: String,
                                                      internal val onDestroyedCallback: ((StandardActivityLifecycleCallbacks) -> Unit)? = null): Application.ActivityLifecycleCallbacks {
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

private var uniqueValue1: Long = 0
private fun generateCallerKey(): String {
    val key = CALLER_KEY + uniqueValue1

    uniqueValue1++

    return key
}

class ActivityLauncher<T: Parcelable>: Launcher<ActivityParams, T>(
    { params, continuation ->
        val action = PREFIX + uniqueValue
        val callerId = generateCallerKey()

        var result: Parcelable? = null

        val broadcastReceiver =  object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                context.unregisterReceiver(this)

                result = intent.getParcelableExtra(RESULT)
            }
        }

        params.callingActivity.intent.putExtra(CALLER_KEY, callerId)
        params.callingActivity.application.registerActivityLifecycleCallbacks(object :
            StandardActivityLifecycleCallbacks(broadcastReceiver,
                activityIntentKey = ACTION_KEY,
                activityIntentValue = action,
                action = action,
                callerId = callerId) {
            private fun handleBundle(bundle: Bundle?, activity: Activity) {
                val cancelUnregisterCallbacks = object: StandardActivityLifecycleCallbacks(this.broadcastReceiver,
                    activityIntentKey = this.activityIntentKey,
                    activityIntentValue = this.activityIntentValue,
                    action = this.action,
                    callerId = this.callerId,
                    onDestroyedCallback = {
                        activity.application.unregisterActivityLifecycleCallbacks(it)
                        continuation.cancel()
                    }){
                }
                if (bundle?.containsKey(CALLER_KEY) == true) {
                    result?.let {
                        continuation.resume(it as T)
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

        params.intent.putExtra(ACTION_KEY, action)

        params.callingActivity.startActivity(params.intent)
    }
)

class FragmentLauncher<T: Parcelable>: Launcher<FragmentParams, T>(
    { params, continuation ->
        if (params.fragment.arguments == null) {
            params.fragment.arguments = Bundle()
        }

        val action = PREFIX + uniqueValue
        val callerId = generateCallerKey()
        params.callingActivity.intent.putExtra(CALLER_KEY, callerId)

        val application = params.callingActivity.application

        var activityCallbacks: Application.ActivityLifecycleCallbacks? = null

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
                        continuation.resume(f.arguments!!.getParcelable(RESULT))
                    } else {
                        continuation.cancel()
                    }
                }
            }
        }

        activityCallbacks = object : StandardActivityLifecycleCallbacks(null,
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

        params.fragmentManager.registerFragmentLifecycleCallbacks(fragmentCallbacks, false)

        params.callingActivity.application.registerActivityLifecycleCallbacks(activityCallbacks)
        params.fragment.arguments!!.putString(ACTION_KEY, action)

        params.fragmentTransaction.commitAllowingStateLoss()

        uniqueValue++
    }
)

fun <T: Parcelable> createActivityLauncher(): ActivityLauncher<T> = ActivityLauncher()
fun <T: Parcelable> createFragmentLauncher(): FragmentLauncher<T> = FragmentLauncher()