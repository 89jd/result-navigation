package me.jackles.resultnavigation.sample

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import me.jackles.resultnavigation.endWithResult

class ResultFragment: Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return TextView(activity).apply {
            text = "hello world"
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        endWithResult(this@ResultFragment, ResultActivity.Test("Hello"))
        GlobalScope.apply {
            launch(Dispatchers.Main) {
                fragmentManager?.popBackStackImmediate()
            }
        }
    }
}