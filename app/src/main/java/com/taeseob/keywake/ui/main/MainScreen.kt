package com.taeseob.keywake.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.taeseob.keywake.AddEditAlarm
import com.taeseob.keywake.KeyWakeApplication
import com.taeseob.keywake.Settings
import com.taeseob.keywake.data.Alarm
import com.taeseob.keywake.data.MissionType
import com.taeseob.keywake.util.AlarmScheduler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as? KeyWakeApplication
    val repository = app?.alarmRepository

    val viewModel: MainScreenViewModel = viewModel {
        MainScreenViewModel(repository!!)
    }

    val alarms by viewModel.alarms.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Key-Wake", fontWeight = FontWeight.Bold, fontSize = 24.sp) },
                actions = {
                    IconButton(onClick = { onItemClick(Settings) }) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "설정")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onItemClick(AddEditAlarm(alarmId = null)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 16.dp, end = 8.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "알람 추가", modifier = Modifier.size(28.dp))
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (alarms.isEmpty()) {
                EmptyAlarmsView(onAddClick = { onItemClick(AddEditAlarm(alarmId = null)) })
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(top = 10.dp, bottom = 80.dp)
                ) {
                    items(alarms, key = { it.id }) { alarm ->
                        AlarmCard(
                            alarm = alarm,
                            onToggle = { isEnabled ->
                                viewModel.toggleAlarm(alarm, isEnabled)
                                val updatedAlarm = alarm.copy(isEnabled = isEnabled)
                                if (isEnabled) {
                                    AlarmScheduler.schedule(context, updatedAlarm)
                                } else {
                                    AlarmScheduler.cancel(context, updatedAlarm)
                                }
                            },
                            onClick = { onItemClick(AddEditAlarm(alarmId = alarm.id)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AlarmCard(
    alarm: Alarm,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (alarm.isEnabled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Alarm time
                Text(
                    text = alarm.getFormattedTime(),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (alarm.isEnabled) MaterialTheme.colorScheme.onBackground
                    else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Days and output details
                val infoText = buildString {
                    append(alarm.getDaysOfWeekString())
                    if (alarm.label.isNotEmpty()) {
                        append(" • ${alarm.label}")
                    }
                    if (alarm.isVibrateOnly) {
                        append(" (진동 전용)")
                    }
                }
                Text(
                    text = infoText,
                    fontSize = 13.sp,
                    color = if (alarm.isEnabled) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                // Mission Badge
                MissionBadge(missionType = alarm.missionType, isEnabled = alarm.isEnabled)
            }

            // Toggle switch
            Switch(
                checked = alarm.isEnabled,
                onCheckedChange = onToggle
            )
        }
    }
}

@Composable
fun MissionBadge(missionType: MissionType, isEnabled: Boolean) {
    val alpha = if (isEnabled) 1f else 0.4f
    val containerColor = when (missionType) {
        MissionType.NONE -> MaterialTheme.colorScheme.surface
        MissionType.TEXT -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f * alpha)
        MissionType.STEP_COUNTER -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f * alpha)
        MissionType.VOICE -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f * alpha)
    }
    val contentColor = when (missionType) {
        MissionType.NONE -> MaterialTheme.colorScheme.onSurface
        MissionType.TEXT -> MaterialTheme.colorScheme.primary
        MissionType.STEP_COUNTER -> MaterialTheme.colorScheme.secondary
        MissionType.VOICE -> MaterialTheme.colorScheme.tertiary
    }.copy(alpha = alpha)

    val icon = when (missionType) {
        MissionType.NONE -> Icons.Default.NotificationsNone
        MissionType.TEXT -> Icons.Default.Keyboard
        MissionType.STEP_COUNTER -> Icons.Default.DirectionsWalk
        MissionType.VOICE -> Icons.Default.Mic
    }

    val label = when (missionType) {
        MissionType.NONE -> "미션 없음"
        MissionType.TEXT -> "글자 받아쓰기"
        MissionType.STEP_COUNTER -> "걷기 이동"
        MissionType.VOICE -> "음성 구절 말하기"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = contentColor, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, color = contentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun EmptyAlarmsView(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "⏰",
            fontSize = 64.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = "설정된 알람이 없습니다.",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "미션을 통해 아침 기상과 해야 할 행동을 강제해 보세요!",
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onAddClick,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("첫 알람 등록하기", fontWeight = FontWeight.Bold)
        }
    }
}
