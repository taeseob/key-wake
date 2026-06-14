package com.example.key_wake.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.key_wake.data.Alarm
import com.example.key_wake.receiver.AlarmReceiver
import java.util.Calendar

object AlarmScheduler {

    fun schedule(context: Context, alarm: Alarm) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        if (!alarm.isEnabled) {
            cancel(context, alarm)
            return
        }

        val triggerTime = calculateNextTriggerTime(alarm)
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", alarm.id)
        }
        
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.hashCode(),
            intent,
            flags
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    fun cancel(context: Context, alarm: Alarm) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_NO_CREATE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.hashCode(),
            intent,
            flags
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun calculateNextTriggerTime(alarm: Alarm): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val now = Calendar.getInstance()

        if (alarm.daysOfWeek.isEmpty()) {
            // Non-repeating alarm
            if (calendar.before(now)) {
                calendar.add(Calendar.DAY_OF_YEAR, 1) // If time has passed today, schedule for tomorrow
            }
            return calendar.timeInMillis
        } else {
            // Repeating alarm
            var minTriggerTime = Long.MAX_VALUE
            val currentDayOfWeek = now.get(Calendar.DAY_OF_WEEK) // 1: Sun, 2: Mon, ..., 7: Sat
            
            // Map Calendar.DAY_OF_WEEK (Sun=1, Mon=2...) to our format (Mon=1, Tue=2..., Sun=7)
            val currentDayMapped = when (currentDayOfWeek) {
                Calendar.SUNDAY -> 7
                else -> currentDayOfWeek - 1
            }

            for (day in alarm.daysOfWeek) {
                val tempCal = calendar.clone() as Calendar
                val daysDiff = day - currentDayMapped
                if (daysDiff < 0 || (daysDiff == 0 && tempCal.before(now))) {
                    tempCal.add(Calendar.DAY_OF_YEAR, daysDiff + 7)
                } else {
                    tempCal.add(Calendar.DAY_OF_YEAR, daysDiff)
                }
                if (tempCal.timeInMillis < minTriggerTime) {
                    minTriggerTime = tempCal.timeInMillis
                }
            }
            return minTriggerTime
        }
    }
}
