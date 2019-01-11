package me.jackles.resultnavigation

import android.app.Activity
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
private const val CALLER_KEY ="$PREFIX.caller"
private const val ACTION_KEY ="$PREFIX.action.key"
private const val SAVE_INSTANCE_KEY ="$PREFIX.action.save.instance"
private var uniqueValue = 0L

fun <T: Parcelable>launchForResult(cancellableContinuation: CancellableContinuation<T>,
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

fun <T: Parcelable>launchForResult(cancellableContinuation: CancellableContinuation<T>,
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
    Handler(Looper.getMainLooper())
        .post {
            fragment.activity?.let { activity ->
                fragment.arguments?.putBoolean(HAS_RESULT, true)
                activity.sendBroadcast(Intent(fragment.arguments?.getString(ACTION_KEY))
                    .putExtra(RESULT, result))
                fragment.fragmentManager?.popBackStackImmediate()
            }
        }
}


private val RESULTS_FOR_DESTROYED_ACTIVITIES = mutableMapOf<String, Parcelable>()

class LaunchForResult {
    private fun <T : Parcelable> broadcastReceiverFactory(
        continuation: CancellableContinuation<T>,
        callerKey: String
    ): BroadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                context.unregisterReceiver(this)
                hasReceivedBroadcast = true

                val result = intent.getParcelableExtra<T>(RESULT)
                if (hasCallerBeenDestroyed) {
                    continuation.cancel()
                    RESULTS_FOR_DESTROYED_ACTIVITIES[callerKey] = result
                } else {
                    continuation.resume(intent.getParcelableExtra(RESULT))
                }
            }
        }

    private var hasCallerBeenDestroyed = false
    private var hasReceivedBroadcast = false
    private lateinit var broadcastReceiver: BroadcastReceiver

    @Synchronized
    fun <T: Parcelable> execute(
        continuation: CancellableContinuation<T>,
        callingActivity: FragmentActivity,
        intent: Intent
    ) {
        val callingActivityCallerId = callingActivity.intent.getStringExtra(CALLER_KEY)
        if (callingActivityCallerId != null && RESULTS_FOR_DESTROYED_ACTIVITIES.containsKey(callingActivityCallerId)) {
            RESULTS_FOR_DESTROYED_ACTIVITIES.remove(callingActivityCallerId)
            continuation.resume(RESULTS_FOR_DESTROYED_ACTIVITIES[callingActivityCallerId] as T)
        } else {
            val action = PREFIX + uniqueValue
            val callerId = CALLER_KEY + uniqueValue
            broadcastReceiver = broadcastReceiverFactory(
                continuation,
                callerId
            )

            callingActivity.intent.putExtra(CALLER_KEY, callerId)
            callingActivity.application.registerActivityLifecycleCallbacks(object :
                Application.ActivityLifecycleCallbacks {
                override fun onActivityPaused(activity: Activity?) {}

                override fun onActivityResumed(activity: Activity?) {}

                override fun onActivityDestroyed(activity: Activity) {
                    if (activity.intent.hasExtra(callerId)) {
                        hasCallerBeenDestroyed = true
                    }
                    if (activity.intent.getStringExtra(ACTION_KEY) == action) {
                        if (!activity.intent.hasExtra(HAS_RESULT)) {
                            continuation.cancel()
                        }
                        activity.application.unregisterActivityLifecycleCallbacks(this)
                    }
                }

                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
                    outState.putBoolean(SAVE_INSTANCE_KEY, true)
                }

                override fun onActivityStarted(activity: Activity) {
                    if (activity.intent.getStringExtra(ACTION_KEY) == action) {
                        activity.registerReceiver(broadcastReceiver, IntentFilter(action))
                    }
                }

                override fun onActivityStopped(activity: Activity) {
                    if (activity.intent.getStringExtra(ACTION_KEY) == action) {
                        if (!activity.intent.hasExtra(HAS_RESULT)) {
                            activity.unregisterReceiver(broadcastReceiver)
                        }
                    }
                }

                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                    if (activity.intent.getStringExtra(ACTION_KEY) == action) {
                        if (savedInstanceState != null && savedInstanceState.containsKey(SAVE_INSTANCE_KEY)) {
                            activity.application.registerActivityLifecycleCallbacks(this)
                            savedInstanceState.remove(SAVE_INSTANCE_KEY)
                        }
                    }
                }

            })

            intent.putExtra(ACTION_KEY, action)

            callingActivity.startActivity(intent)

            uniqueValue++
        }
    }

    @Synchronized
    fun <T: Parcelable> execute(
        continuation: CancellableContinuation<T>,
        callingActivity: FragmentActivity,
        callingFragment: Fragment,
        fragmentManager: FragmentManager,
        fragmentTransaction: FragmentTransaction,
        _bundle: Bundle? = null
    ) {
        val bundle = _bundle ?: Bundle()

        callingFragment.arguments = bundle

        broadcastReceiver = broadcastReceiverFactory(continuation, "")

        val action = PREFIX + uniqueValue

        fragmentManager.registerFragmentLifecycleCallbacks(object:
            FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentDestroyed(fm: FragmentManager, f: Fragment) {
                f.arguments?.let {
                    if (it.getString(ACTION_KEY) == action) {
                        if (!it.containsKey(HAS_RESULT)) {
                            callingActivity.unregisterReceiver(broadcastReceiver)
                            continuation.cancel()
                        }
                        fragmentManager.unregisterFragmentLifecycleCallbacks(this)
                    }
                }
            }
        }, false)

        callingActivity.registerReceiver(broadcastReceiver, IntentFilter(action))

        bundle.putString(ACTION_KEY, action)

        fragmentTransaction.commitAllowingStateLoss()

        uniqueValue++
    }
}