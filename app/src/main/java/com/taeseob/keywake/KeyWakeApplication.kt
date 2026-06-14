package com.taeseob.keywake

import android.app.Application
import com.taeseob.keywake.data.AlarmRepository

class KeyWakeApplication : Application() {
    lateinit var alarmRepository: AlarmRepository
        private set

    override fun onCreate() {
        super.onCreate()
        alarmRepository = AlarmRepository(this)
    }
}
