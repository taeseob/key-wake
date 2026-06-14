package com.taeseob.keywake

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Main : NavKey
@Serializable data class AddEditAlarm(val alarmId: String? = null) : NavKey
@Serializable data object Settings : NavKey
