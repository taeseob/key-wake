package com.example.key_wake.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AlarmRepository(private val context: Context) {
    private val sharedPreferences = context.getSharedPreferences("key_wake_alarms", Context.MODE_PRIVATE)
    private val _alarms = MutableStateFlow<List<Alarm>>(emptyList())
    val alarmsFlow: Flow<List<Alarm>> = _alarms.asStateFlow()

    init {
        loadAlarms()
    }

    private fun loadAlarms() {
        val alarmsJson = sharedPreferences.getString("alarms_list", "[]") ?: "[]"
        try {
            val list = Json.decodeFromString<List<Alarm>>(alarmsJson)
            _alarms.value = list
        } catch (e: Exception) {
            _alarms.value = emptyList()
        }
    }

    fun getAlarms(): List<Alarm> = _alarms.value

    fun getAlarm(id: String): Alarm? = _alarms.value.find { it.id == id }

    fun saveAlarm(alarm: Alarm) {
        val currentList = _alarms.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == alarm.id }
        if (index >= 0) {
            currentList[index] = alarm
        } else {
            currentList.add(alarm)
        }
        updateAlarms(currentList)
    }

    fun deleteAlarm(alarmId: String) {
        val currentList = _alarms.value.filter { it.id != alarmId }
        updateAlarms(currentList)
    }

    private fun updateAlarms(newList: List<Alarm>) {
        _alarms.value = newList
        val json = Json.encodeToString(newList)
        sharedPreferences.edit().putString("alarms_list", json).apply()
    }

    fun getTheme(): String {
        return sharedPreferences.getString("app_theme", "DEFAULT") ?: "DEFAULT"
    }

    fun saveTheme(theme: String) {
        sharedPreferences.edit().putString("app_theme", theme).apply()
    }
}
