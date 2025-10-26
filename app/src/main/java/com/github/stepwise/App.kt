package com.github.stepwise

import android.app.Application
import com.github.stepwise.network.ApiClient

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        ApiClient.init(this)
    }
}