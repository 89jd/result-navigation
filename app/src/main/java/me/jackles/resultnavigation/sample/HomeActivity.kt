package me.jackles.resultnavigation.sample

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class HomeActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GlobalScope.apply {
            launch {
                println(Navigator(this@HomeActivity).launchGetStringViaActivity())
                println("Navigated activity")
            }
        }

        GlobalScope.apply {
            launch {
                println(Navigator(this@HomeActivity).launchGetStringViaFragment())
                println("Navigated fragment")
            }
        }
    }
}