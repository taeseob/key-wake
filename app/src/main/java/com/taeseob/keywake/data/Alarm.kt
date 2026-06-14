package com.taeseob.keywake.data

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class MissionType {
    NONE, TEXT, STEP_COUNTER, VOICE
}

@Serializable
data class Alarm(
    val id: String = UUID.randomUUID().toString(),
    val hour: Int,
    val minute: Int,
    val daysOfWeek: List<Int> = emptyList(), // 1: Mon, 2: Tue, ..., 7: Sun
    val label: String = "",
    val isEnabled: Boolean = true,
    val isVibrateOnly: Boolean = false,
    val soundUri: String? = null,
    val isVibrationEnabled: Boolean = true,
    val missionType: MissionType = MissionType.NONE,
    val missionTextTarget: String = "",
    val missionStepCountTarget: Int = 30
) {
    val isRepeating: Boolean
        get() = daysOfWeek.isNotEmpty()

    fun getFormattedTime(): String {
        val amPm = if (hour < 12) "오전" else "오후"
        val formattedHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return String.format("%s %d:%02d", amPm, formattedHour, minute)
    }

    fun getDaysOfWeekString(): String {
        if (daysOfWeek.isEmpty()) return "한 번만"
        if (daysOfWeek.size == 7) return "매일"
        val dayNames = listOf("월", "화", "수", "목", "금", "토", "일")
        return daysOfWeek.sorted().joinToString(", ") { dayNames[it - 1] }
    }
}
