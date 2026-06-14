package com.taeseob.keywake

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.taeseob.keywake.ui.alarm.AddEditAlarmScreen
import com.taeseob.keywake.ui.main.MainScreen
import com.taeseob.keywake.ui.settings.SettingsScreen

@Composable
fun MainNavigation() {
  val backStack = rememberNavBackStack(Main)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Main> {
          MainScreen(
              onItemClick = { navKey -> backStack.add(navKey) },
              modifier = Modifier.fillMaxSize()
          )
        }
        entry<AddEditAlarm> { key ->
          AddEditAlarmScreen(
              alarmId = key.alarmId,
              onBack = { backStack.removeLastOrNull() },
              modifier = Modifier.fillMaxSize()
          )
        }
        entry<Settings> {
          SettingsScreen(
              onBack = { backStack.removeLastOrNull() },
              modifier = Modifier.fillMaxSize()
          )
        }
      },
  )
}
