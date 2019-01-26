package me.jackles.resultnavigation.sample

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import me.jackles.resultnavigation.ActivityParams
import me.jackles.resultnavigation.FragmentParams
import me.jackles.resultnavigation.createActivityLauncher
import me.jackles.resultnavigation.createFragmentLauncher

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
                this@Navigator.activity = activity as FragmentActivity
            }
        })
    }

    suspend fun launchGetStringViaActivity(): ResultActivity.Test =
        createActivityLauncher<ResultActivity.Test>()
            .launchForResult(ActivityParams(activity!!, Intent(this.activity, ResultActivity::class.java))) {
                println("Cancelled")
            }

    @SuppressLint("CommitTransaction")
    suspend fun launchGetStringViaFragment() =
        ResultFragment().let {
            createFragmentLauncher<ResultActivity.Test>().launchForResult(FragmentParams(activity!!,
                it,
                activity!!.supportFragmentManager,
                activity!!.supportFragmentManager.beginTransaction()
                    .replace(
                        android.R.id.content,
                        it, "")
                    .addToBackStack(""))) {
                println("Cancelled")
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