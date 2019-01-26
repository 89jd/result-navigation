package me.jackles.resultnavigation.sample

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.LinearLayout
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import me.jackles.resultnavigation.common.ChoiceResult
import me.jackles.resultnavigation.common.getValue

class HomeActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Navigator.init(application)
        setContentView(LinearLayout(this).apply {
            addView(Button(this@HomeActivity).apply {
                text = "Activity"
                setOnClickListener {
                    GlobalScope.apply {
                        launch {
                            val r = navigator.launchGetStringViaActivity()
                            if (r is ChoiceResult.OK<*>) {
                                println(r.getValue<ResultActivity.Test>())
                            } else {
                                println(r)
                            }
                            println("Complete suspended function")
                        }
                    }
                }
            })
            addView(Button(this@HomeActivity).apply {
                text = "Fragment"
                setOnClickListener {
                    GlobalScope.apply {
                        launch {
                            println(navigator.launchGetStringViaFragment())
                            println("Complete suspended function")
                        }
                    }
                }
            })
        })
    }

    override fun onResume() {
        super.onResume()
    }
}