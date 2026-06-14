package com.example.key_wake

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.key_wake.theme.KeyWakeTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    requestPermissions()

    val app = applicationContext as? KeyWakeApplication
    val repository = app?.alarmRepository
    val initialTheme = repository?.getTheme() ?: "DEFAULT"

    enableEdgeToEdge()
    setContent {
      KeyWakeTheme(themeName = initialTheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          MainNavigation()
        }
      }
    }
  }

  private fun requestPermissions() {
    val permissions = mutableListOf<String>()
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      permissions.add(Manifest.permission.POST_NOTIFICATIONS)
    }
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      permissions.add(Manifest.permission.ACTIVITY_RECOGNITION)
    }

    permissions.add(Manifest.permission.RECORD_AUDIO)

    val listToRequest = permissions.filter {
      ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
    }

    if (listToRequest.isNotEmpty()) {
      ActivityCompat.requestPermissions(this, listToRequest.toTypedArray(), 101)
    }
  }
}
