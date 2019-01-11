package me.jackles.resultnavigation.sample

import android.R
import android.annotation.SuppressLint
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import me.jackles.resultnavigation.launchForResult

class Navigator(val activity: AppCompatActivity) {
    suspend fun launchGetStringViaActivity() = suspendCancellableCoroutine<ResultActivity.Test> {
        launchForResult(it,
            activity,
            Intent(activity, ResultActivity::class.java)
        )
    }

    @SuppressLint("CommitTransaction")
    suspend fun launchGetStringViaFragment() = suspendCancellableCoroutine<ResultActivity.Test> {
        val frag = ResultFragment()
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