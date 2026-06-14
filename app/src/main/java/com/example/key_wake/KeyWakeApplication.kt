package com.example.key_wake

import android.app.Application
import com.example.key_wake.data.AlarmRepository

class KeyWakeApplication : Application() {
    lateinit var alarmRepository: AlarmRepository
        private set

    override fun onCreate() {
        super.onCreate()
        alarmRepository = AlarmRepository(this)
    }
}
