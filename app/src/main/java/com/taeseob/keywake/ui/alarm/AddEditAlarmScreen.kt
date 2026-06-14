package com.taeseob.keywake.ui.alarm

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taeseob.keywake.KeyWakeApplication
import com.taeseob.keywake.data.Alarm
import com.taeseob.keywake.data.MissionType
import com.taeseob.keywake.util.AlarmScheduler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAlarmScreen(
    alarmId: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as? KeyWakeApplication
    val repository = app?.alarmRepository

    // Fetch existing alarm or prepare new one
    val existingAlarm = remember(alarmId) {
        alarmId?.let { repository?.getAlarm(it) }
    }

    var hour by remember { mutableIntStateOf(existingAlarm?.hour ?: 7) }
    var minute by remember { mutableIntStateOf(existingAlarm?.minute ?: 0) }
    var label by remember { mutableStateOf(existingAlarm?.label ?: "") }
    var daysOfWeek by remember { mutableStateOf(existingAlarm?.daysOfWeek?.toSet() ?: emptySet()) }
    
    var isVibrateOnly by remember { mutableStateOf(existingAlarm?.isVibrateOnly ?: false) }
    var isVibrationEnabled by remember { mutableStateOf(existingAlarm?.isVibrationEnabled ?: true) }
    
    var missionType by remember { mutableStateOf(existingAlarm?.missionType ?: MissionType.NONE) }
    var missionTextTarget by remember { mutableStateOf(existingAlarm?.missionTextTarget ?: "") }
    var missionStepCountTarget by remember { mutableIntStateOf(existingAlarm?.missionStepCountTarget ?: 30) }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existingAlarm == null) "알람 추가" else "알람 편집", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "뒤로")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (existingAlarm != null) {
                    OutlinedButton(
                        onClick = {
                            repository?.deleteAlarm(existingAlarm.id)
                            AlarmScheduler.cancel(context, existingAlarm)
                            Toast.makeText(context, "알람이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                            onBack()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = borderStroke(1.dp, MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                    ) {
                        Text("삭제", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                Button(
                    onClick = {
                        // Validate inputs
                        if (missionType == MissionType.TEXT && missionTextTarget.trim().isEmpty()) {
                            Toast.makeText(context, "미션 문구를 입력해 주세요.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (missionType == MissionType.VOICE && missionTextTarget.trim().isEmpty()) {
                            Toast.makeText(context, "음성 인식 문구를 입력해 주세요.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val newAlarm = Alarm(
                            id = existingAlarm?.id ?: java.util.UUID.randomUUID().toString(),
                            hour = hour,
                            minute = minute,
                            label = label,
                            daysOfWeek = daysOfWeek.toList().sorted(),
                            isEnabled = true,
                            isVibrateOnly = isVibrateOnly,
                            isVibrationEnabled = isVibrationEnabled,
                            missionType = missionType,
                            missionTextTarget = missionTextTarget,
                            missionStepCountTarget = missionStepCountTarget
                        )
                        
                        repository?.saveAlarm(newAlarm)
                        AlarmScheduler.schedule(context, newAlarm)
                        
                        Toast.makeText(context, "알람이 저장되었습니다.", Toast.LENGTH_SHORT).show()
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(2f)
                        .height(56.dp)
                ) {
                    Text("저장", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Time Picker Grid
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("시간 설정", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Hour Control
                        NumberPicker(value = hour, onValueChange = { hour = it }, range = 0..23, label = "시")
                        Text(text = ":", fontSize = 36.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp))
                        // Minute Control
                        NumberPicker(value = minute, onValueChange = { minute = it }, range = 0..59, label = "분")
                    }
                }
            }

            // Days of Week Repeater Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text("요일 반복 설정", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val days = listOf("월" to 1, "화" to 2, "수" to 3, "목" to 4, "금" to 5, "토" to 6, "일" to 7)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        days.forEach { (name, value) ->
                            val isSelected = daysOfWeek.contains(value)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) Color.Transparent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                        CircleShape
                                    )
                                    .clickable {
                                        daysOfWeek = if (isSelected) {
                                            daysOfWeek - value
                                        } else {
                                            daysOfWeek + value
                                        }
                                    }
                            ) {
                                Text(
                                    text = name,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Alarm Sound & Vibration Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("출력 방식 설정", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("진동으로만 울림", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Switch(
                            checked = isVibrateOnly,
                            onCheckedChange = { isVibrateOnly = it }
                        )
                    }
                    
                    if (!isVibrateOnly) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("진동 병행", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            Switch(
                                checked = isVibrationEnabled,
                                onCheckedChange = { isVibrationEnabled = it }
                            )
                        }
                    }
                }
            }

            // Alarm Label Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("알람 이름", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        placeholder = { Text("예: 출근 준비 및 걷기 미션") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Mission Configuration Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("해제 미션 선택", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Mission Type Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val missions = listOf(
                            MissionType.NONE to "일반",
                            MissionType.TEXT to "글자 쓰기",
                            MissionType.STEP_COUNTER to "걷기",
                            MissionType.VOICE to "음성 말하기"
                        )
                        
                        missions.forEach { (type, name) ->
                            val isSelected = missionType == type
                            FilterChip(
                                selected = isSelected,
                                onClick = { missionType = type },
                                label = { Text(name, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Conditional settings depending on MissionType
                    when (missionType) {
                        MissionType.NONE -> {
                            Text(
                                "특별한 행동 없이 알람 해제 버튼 클릭 시 즉시 정지됩니다.",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        MissionType.TEXT -> {
                            Text("해제할 텍스트 문장 입력", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = missionTextTarget,
                                onValueChange = { missionTextTarget = it },
                                placeholder = { Text("예: 오늘도 화이팅하자!") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        MissionType.STEP_COUNTER -> {
                            Text("기상 후 목표 걸음 수", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val steps = listOf(10, 30, 50, 100)
                                steps.forEach { count ->
                                    val isSelected = missionStepCountTarget == count
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                                            .border(1.dp, if (isSelected) Color.Transparent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                            .clickable { missionStepCountTarget = count }
                                    ) {
                                        Text(
                                            text = "$count 걸음",
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                        MissionType.VOICE -> {
                            Text("따라 말할 단어/구절 입력", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = missionTextTarget,
                                onValueChange = { missionTextTarget = it },
                                placeholder = { Text("예: 일어나서 물 한잔 마시자") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
                                border = borderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "⚠️ 음성 해제 미션 안내",
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "음성 인식은 오프라인(비행기 모드 등)에서 제대로 작동하지 않을 수 있습니다. 설정에서 오프라인 음성 인식 언어팩을 미리 받아두시거나, 오프라인 시 일반 알람(2분 뒤 자동 종료 및 오프라인 알람 정지 기능 활성화)으로 전환되어 동작합니다.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun NumberPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
        ) {
            IconButton(
                onClick = {
                    val prev = value - 1
                    if (prev in range) onValueChange(prev) else onValueChange(range.last)
                }
            ) {
                Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                text = String.format("%02d", value),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(60.dp)
            )
            IconButton(
                onClick = {
                    val next = value + 1
                    if (next in range) onValueChange(next) else onValueChange(range.first)
                }
            ) {
                Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Utility to handle BorderStroke in Compose
@Composable
fun borderStroke(width: androidx.compose.ui.unit.Dp, color: Color) = 
    androidx.compose.foundation.BorderStroke(width, color)
