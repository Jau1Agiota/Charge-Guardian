package com.chargeguardian.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity() {
    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { recreate() }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF9AE7C2), background = Color(0xFF101318), surface = Color(0xFF191E25))) {
                val vm: GuardianViewModel = viewModel()
                val settings by vm.settings.collectAsStateWithLifecycle()
                val battery by vm.battery.collectAsStateWithLifecycle()
                val message by vm.message.collectAsStateWithLifecycle()
                val notifications by vm.notifications.collectAsStateWithLifecycle()
                val running by vm.running.collectAsStateWithLifecycle()
                androidx.lifecycle.compose.LifecycleResumeEffect(Unit) { vm.refresh(); onPauseOrDispose {} }
                var slider by remember(settings.threshold) { mutableFloatStateOf(settings.threshold.toFloat()) }
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Text("CHARGE GUARDIAN", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge, letterSpacing = 2.sp)
                        Text("A little less charge.\nA little more care.", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text(if (battery.percentage < 0) "—" else "${battery.percentage}%", fontSize = 68.sp, fontWeight = FontWeight.Light)
                                Text(if (battery.connected) "Charger connected" else "Disconnected", color = MaterialTheme.colorScheme.primary)
                                LinearProgressIndicator(progress = { battery.percentage.coerceIn(0,100) / 100f }, modifier = Modifier.fillMaxWidth())
                                Text("Alert at ${settings.threshold}%", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        Column {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Battery alert threshold", style = MaterialTheme.typography.titleMedium)
                                Text("${slider.toInt()}%", color = MaterialTheme.colorScheme.primary)
                            }
                            Slider(value = slider, onValueChange = { slider = it }, valueRange = 1f..100f, steps = 98,
                                onValueChangeFinished = { vm.threshold(slider.toInt()) })
                            Text("One alert per charging session. Unplug to reset.", style = MaterialTheme.typography.bodySmall)
                        }
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text("Monitoring", style = MaterialTheme.typography.titleMedium)
                                        Text(if (!settings.enabled) "Paused" else if (!notifications) "Notifications blocked" else if (running) "Active • waiting for battery events" else "Restart needed • enable again", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Switch(checked = settings.enabled && running, onCheckedChange = { vm.monitoring(it) })
                                }
                                Text("Enable once while this app is open. An ongoing notification keeps monitoring visible, even after you close this screen.", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (!notifications) Button(onClick = {
                            if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED)
                                permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            else startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
                        }, modifier = Modifier.fillMaxWidth()) { Text("Allow notifications") }
                        TextButton(onClick = {
                            startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
                        }, modifier = Modifier.fillMaxWidth()) { Text("Notification settings") }
                        OutlinedButton(onClick = vm::test, modifier = Modifier.fillMaxWidth()) { Text("Send Test Notification") }
                        if (message.isNotEmpty()) Text(message, color = MaterialTheme.colorScheme.primary)
                        HorizontalDivider()
                        Text("Connect your watch", style = MaterialTheme.typography.titleLarge)
                        Text("In Zepp, open your Amazfit Active 2 → Notifications and reminders → App alerts. Grant Zepp notification access, then enable Charge Guardian. Keep Zepp connected and allowed to run in the background.", style = MaterialTheme.typography.bodyMedium)
                        Text("On Xiaomi / HyperOS", style = MaterialTheme.typography.titleMedium)
                        Text("Enable Background autostart for Charge Guardian and Zepp. Set battery management to No restrictions for both. Menu names vary by HyperOS version. Reopen Charge Guardian after force-stop or if monitoring disappears.", style = MaterialTheme.typography.bodyMedium)
                        Text("Private by design • No network access\nVersion 1.0.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
