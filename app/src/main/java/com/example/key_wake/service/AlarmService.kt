package com.example.key_wake.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.key_wake.KeyWakeApplication
import android.media.RingtoneManager
import com.example.key_wake.data.Alarm
import com.example.key_wake.ui.alarm.AlarmActivity

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var audioManager: AudioManager? = null
    private var alarm: Alarm? = null

    private var isMuted = false

    companion object {
        const val CHANNEL_ID = "key_wake_alarm_channel"
        const val NOTIFICATION_ID = 9999
        var activeService: AlarmService? = null
    }

    override fun onCreate() {
        super.onCreate()
        activeService = this
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "KeyWake::AlarmServiceWakeLock").apply {
            acquire(10 * 60 * 1000) // Max 10 minutes wake lock
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getStringExtra("ALARM_ID")
        val app = applicationContext as? KeyWakeApplication
        alarm = alarmId?.let { app?.alarmRepository?.getAlarm(it) }

        if (alarm == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        requestAudioFocusAndPlay()

        return START_STICKY
    }

    private fun requestAudioFocusAndPlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build())
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
            
            val result = audioManager?.requestAudioFocus(focusRequest)
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                startMediaAndVibration()
            }
        } else {
            @Suppress("DEPRECATION")
            val result = audioManager?.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_ALARM,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            )
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                startMediaAndVibration()
            }
        }
    }

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT, AudioManager.AUDIOFOCUS_LOSS -> {
                muteAlarm()
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                unmuteAlarm()
            }
        }
    }

    private fun startMediaAndVibration() {
        if (isMuted) return

        val currentAlarm = alarm ?: return

        // 1. Play Alarm Sound (if not vibrate only)
        if (!currentAlarm.isVibrateOnly) {
            try {
                val soundUri = currentAlarm.soundUri?.let { Uri.parse(it) }
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(applicationContext, soundUri)
                    setAudioAttributes(AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build())
                    isLooping = true
                    prepare()
                    start()
                }
            } catch (e: Exception) {
                // If custom sound fails, try system default alarm
                try {
                    val defaultUri = Uri.parse("content://settings/system/alarm_alert")
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(applicationContext, defaultUri)
                        setAudioAttributes(AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build())
                        isLooping = true
                        prepare()
                        start()
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }

        // 2. Start Vibration (if enabled or if vibrate only)
        if (currentAlarm.isVibrateOnly || currentAlarm.isVibrationEnabled) {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            val pattern = longArrayOf(0, 1000, 1000) // Vibrate 1s, pause 1s
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        }
    }

    private fun muteAlarm() {
        isMuted = true
        try {
            mediaPlayer?.setVolume(0f, 0f)
            vibrator?.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun unmuteAlarm() {
        isMuted = false
        try {
            mediaPlayer?.setVolume(1f, 1f)
            // Restart vibration if enabled
            val currentAlarm = alarm
            if (currentAlarm != null && (currentAlarm.isVibrateOnly || currentAlarm.isVibrationEnabled)) {
                val pattern = longArrayOf(0, 1000, 1000)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(pattern, 0)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopAlarm() {
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        activeService = null
        
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Key-Wake 알람 서비스",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "알람 작동 시 화면을 점유하고 미션을 실행하는 포그라운드 서비스 알림"
                setSound(null, null)
                enableVibration(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val alarmId = alarm?.id ?: ""
        val alarmIntent = Intent(this, AlarmActivity::class.java).apply {
            putExtra("ALARM_ID", alarmId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            0,
            alarmIntent,
            pendingFlags
        )

        val text = alarm?.label?.takeIf { it.isNotEmpty() } ?: "알람이 작동 중입니다!"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Key-Wake 알람")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true) // Launches AlarmActivity even on lockscreen
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .build()
    }
}
