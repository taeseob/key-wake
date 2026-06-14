package com.taeseob.keywake.ui.alarm

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taeseob.keywake.KeyWakeApplication
import com.taeseob.keywake.data.Alarm
import com.taeseob.keywake.data.MissionType
import com.taeseob.keywake.service.AlarmService
import com.taeseob.keywake.theme.KeyWakeTheme
import java.util.Locale
import kotlin.math.sqrt

class AlarmActivity : ComponentActivity(), SensorEventListener {

    private var alarm: Alarm? = null
    private var sensorManager: SensorManager? = null
    private var stepSensor: Sensor? = null
    private var accelerometer: Sensor? = null
    
    // Step state
    private var initialStepCount = -1
    private var stepsTaken = mutableStateOf(0)
    
    // Accelerometer step detector state
    private var lastAccelerationMagnitude = 0f
    private var lastStepTime = 0L
    private val stepThreshold = 11.8f
    private val stepDebounceTime = 350L // ms
    
    // Speech recognizer state
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = mutableStateOf(false)
    private var spokenText = mutableStateOf("")
    
    // Fallback Timer
    private val fallbackHandler = Handler(Looper.getMainLooper())
    private val autoStopRunnable = Runnable {
        Toast.makeText(this, "제한 시간 초과로 알람이 자동 정지되었습니다.", Toast.LENGTH_LONG).show()
        stopAlarmAndFinish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Show on lock screen and wake up screen
        configureLockScreenFlags()

        val alarmId = intent.getStringExtra("ALARM_ID")
        val app = applicationContext as? KeyWakeApplication
        alarm = alarmId?.let { app?.alarmRepository?.getAlarm(it) }

        if (alarm == null) {
            finish()
            return
        }

        // Initialize Sensors
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        
        registerSensors()

        // Check Offline Voice Mission fallback
        val isOffline = !isNetworkAvailable()
        if (alarm?.missionType == MissionType.VOICE && isOffline) {
            // Set 2-minute auto stop for offline voice mission fallback
            fallbackHandler.postDelayed(autoStopRunnable, 2 * 60 * 1000)
        }

        val themeName = app?.alarmRepository?.getTheme() ?: "DEFAULT"

        enableEdgeToEdge()
        setContent {
            KeyWakeTheme(themeName = themeName) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BackHandler(enabled = true) {
                        // Prevent back press to force mission completion
                    }
                    AlarmScreen(isOffline = isOffline)
                }
            }
        }
    }

    private fun configureLockScreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                    or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun registerSensors() {
        if (alarm?.missionType == MissionType.STEP_COUNTER) {
            if (stepSensor != null) {
                sensorManager?.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI)
            } else if (accelerometer != null) {
                sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (alarm?.missionType != MissionType.STEP_COUNTER) return

        if (event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
            val totalSteps = event.values[0].toInt()
            if (initialStepCount == -1) {
                initialStepCount = totalSteps
            }
            stepsTaken.value = totalSteps - initialStepCount
            checkStepMissionComplete()
        } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val magnitude = sqrt(x * x + y * y + z * z)
            val currentTime = System.currentTimeMillis()
            
            // Basic step detection using peaks
            if (magnitude > stepThreshold && lastAccelerationMagnitude <= stepThreshold) {
                if (currentTime - lastStepTime > stepDebounceTime) {
                    stepsTaken.value += 1
                    lastStepTime = currentTime
                    vibrateFeedback(50)
                    checkStepMissionComplete()
                }
            }
            lastAccelerationMagnitude = magnitude
        }
    }

    private fun checkStepMissionComplete() {
        val target = alarm?.missionStepCountTarget ?: 30
        if (stepsTaken.value >= target) {
            vibrateSuccess()
            stopAlarmAndFinish()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // Speech-To-Text implementation
    private fun startSpeechRecognition() {
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListening.value = true
                        spokenText.value = "듣고 있습니다..."
                    }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        isListening.value = false
                    }
                    override fun onError(error: Int) {
                        isListening.value = false
                        spokenText.value = "인식 실패 (다시 시도해 주세요)"
                    }
                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val resultText = matches[0]
                            spokenText.value = resultText
                            verifyVoiceMission(resultText)
                        } else {
                            spokenText.value = "인식을 할 수 없습니다."
                        }
                    }
                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
        speechRecognizer?.startListening(intent)
    }

    private fun verifyVoiceMission(text: String) {
        val target = alarm?.missionTextTarget ?: ""
        // Calculate basic word matching/similarity
        val cleanText = text.replace(" ", "").lowercase(Locale.getDefault())
        val cleanTarget = target.replace(" ", "").lowercase(Locale.getDefault())
        
        if (cleanText == cleanTarget || cleanText.contains(cleanTarget) || cleanTarget.contains(cleanText)) {
            vibrateSuccess()
            stopAlarmAndFinish()
        } else {
            vibrateFeedback(200)
            Toast.makeText(this, "설정된 구절과 다릅니다. 다시 말씀해 주세요.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopAlarmAndFinish() {
        AlarmService.activeService?.stopAlarm()
        finish()
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val cap = connectivityManager.getNetworkCapabilities(network) ?: return false
            return cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            val info = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            return info?.isConnected == true
        }
    }

    private fun vibrateFeedback(durationMs: Long) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMs)
        }
    }

    private fun vibrateSuccess() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        val pattern = longArrayOf(0, 150, 100, 150, 100, 300)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager?.unregisterListener(this)
        speechRecognizer?.destroy()
        fallbackHandler.removeCallbacks(autoStopRunnable)
    }

    // Compose Screen Layouts
    @Composable
    fun AlarmScreen(isOffline: Boolean) {
        val currentAlarm = alarm ?: return
        val modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .padding(24.dp)
            .safeDrawingPadding()

        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Alarm Title Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 40.dp)
            ) {
                Text(
                    text = currentAlarm.getFormattedTime(),
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = currentAlarm.label.takeIf { it.isNotEmpty() } ?: "알람",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }

            // Central Mission Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                when (currentAlarm.missionType) {
                    MissionType.NONE -> DefaultMissionView()
                    MissionType.TEXT -> TextMissionView(targetText = currentAlarm.missionTextTarget)
                    MissionType.STEP_COUNTER -> StepMissionView(targetSteps = currentAlarm.missionStepCountTarget)
                    MissionType.VOICE -> VoiceMissionView(
                        targetText = currentAlarm.missionTextTarget,
                        isOffline = isOffline
                    )
                }
            }

            // Footer / Guidance
            Text(
                text = "알람을 해제하려면 미션을 완료해 주세요.",
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 20.dp)
            )
        }
    }

    @Composable
    fun DefaultMissionView() {
        Button(
            onClick = { stopAlarmAndFinish() },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .height(60.dp)
                .fillMaxWidth(0.7f)
                .shadow(8.dp, RoundedCornerShape(24.dp))
        ) {
            Text(text = "알람 해제", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }

    @Composable
    fun TextMissionView(targetText: String) {
        var input by remember { mutableStateOf("") }
        val isMatch = input.trim() == targetText.trim()

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "아래 문장을 정확히 받아쓰세요",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            // Target Sentence Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Text(
                    text = targetText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            // Input Field
            BasicTextField(
                value = input,
                onValueChange = { newValue ->
                    input = newValue
                    if (newValue.trim() == targetText.trim()) {
                        vibrateSuccess()
                        stopAlarmAndFinish()
                    }
                },
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        2.dp,
                        if (isMatch) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        RoundedCornerShape(12.dp)
                    )
                    .wrapContentHeight(Alignment.CenterVertically)
                    .padding(horizontal = 16.dp)
            )
        }
    }

    @Composable
    fun StepMissionView(targetSteps: Int) {
        val currentSteps = stepsTaken.value
        val progress = (currentSteps.toFloat() / targetSteps.toFloat()).coerceIn(0f, 1f)
        
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.95f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseScale"
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "일어나서 이동하세요!",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(220.dp)
                    .scale(if (progress < 1f) scale else 1f)
            ) {
                // Background Track
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    strokeWidth = 14.dp,
                )
                // Active Progress
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 14.dp,
                    strokeCap = StrokeCap.Round
                )
                
                // Text inside progress
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$currentSteps",
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "/ $targetSteps 걸음",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    @Composable
    fun VoiceMissionView(targetText: String, isOffline: Boolean) {
        val listening by isListening
        val textSpoken by spokenText
        
        // Microphone Pulsing Animation
        val infiniteTransition = rememberInfiniteTransition(label = "pulseMic")
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.25f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "micScale"
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "지정된 문장을 따라 말하세요",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            // Target Phrase
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Text(
                    text = targetText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            if (isOffline) {
                // Offline Fallback warning & instant turn off button
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "현재 오프라인 상태입니다.",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "음성 인식이 불가능하여 기본 알람으로 전환되었습니다. 2분 후 자동 종료되거나 아래 버튼을 누르면 해제됩니다.",
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { stopAlarmAndFinish() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .height(56.dp)
                        .fillMaxWidth(0.6f)
                ) {
                    Text("알람 즉시 끄기", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                // Spoken output box
                AnimatedVisibility(visible = textSpoken.isNotEmpty()) {
                    Text(
                        text = textSpoken,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                }

                // Microphone button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(
                            if (listening) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                        )
                        .clickable { startSpeechRecognition() }
                        .shadow(4.dp, CircleShape)
                ) {
                    if (listening) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "마이크",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (listening) "듣고 있습니다..." else "누르고 말씀하세요",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
