package me.jackles.resultnavigation

import android.content.Intent
import android.os.Parcelable
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity

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
        fragment.fragmentManager?.popBackStackImmediate()
    }
}
