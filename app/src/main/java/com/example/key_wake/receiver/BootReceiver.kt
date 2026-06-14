package com.example.key_wake.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.key_wake.KeyWakeApplication
import com.example.key_wake.util.AlarmScheduler

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == "android.intent.action.LOCKED_BOOT_COMPLETED") {
            val app = context.applicationContext as? KeyWakeApplication ?: return
            val repository = app.alarmRepository
            val alarms = repository.getAlarms()
            for (alarm in alarms) {
                if (alarm.isEnabled) {
                    AlarmScheduler.schedule(context, alarm)
                }
            }
        }
    }
}
