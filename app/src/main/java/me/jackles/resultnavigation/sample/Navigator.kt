package me.jackles.resultnavigation.sample

import android.R
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import me.jackles.resultnavigation.ChoiceResult
import me.jackles.resultnavigation.launchForResult

class Navigator(val application: Application) {
    private var activity: FragmentActivity? = null

    init {
        application.registerActivityLifecycleCallbacks(object :  Application.ActivityLifecycleCallbacks {
            override fun onActivityPaused(activity: Activity?) {
                this@Navigator.activity = null
            }

            override fun onActivityResumed(activity: Activity) {
                this@Navigator.activity = activity as FragmentActivity
            }

            override fun onActivityDestroyed(activity: Activity) {}

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityStarted(activity: Activity) {}

            override fun onActivityStopped(activity: Activity) {}

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {

            }
        })
    }

    suspend fun launchGetStringViaActivity() = suspendCancellableCoroutine<ChoiceResult<ResultActivity.Test>> {
        activity?.let { activity ->
            launchForResult(it,
                activity,
                Intent(this.activity, ResultActivity::class.java)
            )
        }
    }

    @SuppressLint("CommitTransaction")
    suspend fun launchGetStringViaFragment() = suspendCancellableCoroutine<ChoiceResult<ResultActivity.Test>> {
        val frag = ResultFragment()
        activity?.let { activity ->
            launchForResult(it,
                activity,
                frag,
                activity.supportFragmentManager,
                activity.supportFragmentManager.beginTransaction()
                    .replace(
                        R.id.content,
                        frag, "")
                    .addToBackStack("")
            )
        }
    }

    companion object {
        fun init(application: Application) {
            if (!::navigator.isInitialized) {
                navigator = Navigator(application)
            }
        }
    }
}
lateinit var navigator: Navigator