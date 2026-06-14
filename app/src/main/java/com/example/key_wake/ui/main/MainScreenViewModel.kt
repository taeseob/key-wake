package com.example.key_wake.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.key_wake.data.Alarm
import com.example.key_wake.data.AlarmRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class MainScreenViewModel(private val alarmRepository: AlarmRepository) : ViewModel() {
    val alarms: StateFlow<List<Alarm>> = alarmRepository.alarmsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = alarmRepository.getAlarms()
        )

    fun toggleAlarm(alarm: Alarm, isEnabled: Boolean) {
        val updated = alarm.copy(isEnabled = isEnabled)
        alarmRepository.saveAlarm(updated)
    }
}
