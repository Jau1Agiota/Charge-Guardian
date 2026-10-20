package com.chargeguardian.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.SystemBarStyle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { recreate() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            val vm: GuardianViewModel = viewModel()
            val settings by vm.settings.collectAsStateWithLifecycle()
            val battery by vm.battery.collectAsStateWithLifecycle()
            val message by vm.message.collectAsStateWithLifecycle()
            val notifications by vm.notifications.collectAsStateWithLifecycle()
            val running by vm.running.collectAsStateWithLifecycle()
            var screen by rememberSaveable { mutableStateOf("dashboard") }
            LifecycleResumeEffect(Unit) { vm.refresh(); onPauseOrDispose {} }
            BackHandler(screen != "dashboard") { screen = "dashboard" }

            GuardianTheme(settings.appearance) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.onBackground) {
                    when (screen) {
                        "appearance" -> AppearanceScreen(settings.appearance, vm::appearance) { screen = "dashboard" }
                        "help" -> HelpScreen(::notificationSettings) { screen = "dashboard" }
                        else -> Dashboard(
                            settings = settings, battery = battery, running = running,
                            notifications = notifications, message = message,
                            onThreshold = vm::threshold, onMonitoring = vm::monitoring,
                            onTest = vm::test, onAppearance = { screen = "appearance" },
                            onHelp = { screen = "help" }, onPermission = ::requestNotifications
                        )
                    }
                }
            }
        }
    }

    private fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else notificationSettings()
    }

    private fun notificationSettings() {
        startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
    }
}
