package me.jackles.resultnavigation.sample

import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import me.jackles.resultnavigation.endWithResult

class ResultFragment: Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return TextView(activity).apply {
            setBackgroundColor(Color.RED)
            text = "Frag"
            setOnClickListener {
                endWithResult(this@ResultFragment, ResultActivity.Test("Hello"))
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
}