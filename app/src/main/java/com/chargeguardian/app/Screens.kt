package com.chargeguardian.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
private fun ScreenColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp), content = content
    )
}

@Composable
private fun Panel(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
fun Dashboard(
    settings: Settings, battery: Battery, running: Boolean, notifications: Boolean, message: String,
    onThreshold: (Int) -> Unit, onMonitoring: (Boolean) -> Unit, onTest: () -> Unit,
    onAppearance: () -> Unit, onHelp: () -> Unit, onPermission: () -> Unit
) {
    ScreenColumn {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("CHARGE", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text("GUARDIAN", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, letterSpacing = 3.sp)
            }
            IconButton(onClick = onAppearance, modifier = Modifier.testTag("open_appearance")) {
                Icon(painterResource(R.drawable.ic_palette), contentDescription = "Appearance", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onHelp, modifier = Modifier.testTag("open_help")) {
                Text("?", fontSize = 22.sp, fontWeight = FontWeight.Medium, modifier = Modifier
                    .size(26.dp).border(1.5.dp, MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
                    .wrapContentSize().semantics { contentDescription = "Help" })
            }
        }
        Panel {
            Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { battery.percentage.coerceIn(0, 100) / 100f },
                    modifier = Modifier.size(196.dp).semantics { contentDescription = "Battery charge" },
                    strokeWidth = 7.dp, trackColor = MaterialTheme.colorScheme.outlineVariant,
                    strokeCap = StrokeCap.Round
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("BATTERY", style = MaterialTheme.typography.labelMedium, letterSpacing = 2.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(if (battery.percentage < 0) "—" else "${battery.percentage}%",
                        style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Medium,
                        modifier = Modifier.testTag("battery_percentage"))
                    Text(if (battery.connected) "Charger connected" else "Disconnected",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        ThresholdPanel(settings.threshold, onThreshold)
        Panel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Automatic monitoring", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        when {
                            !settings.enabled -> "Paused"
                            !notifications -> "Notifications blocked"
                            running -> "Active · listening for charging"
                            else -> "Restart needed · enable again"
                        }, style = MaterialTheme.typography.bodySmall,
                        color = if (settings.enabled && notifications && running) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = settings.enabled && running, onCheckedChange = onMonitoring,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        uncheckedTrackColor = MaterialTheme.colorScheme.outlineVariant,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedBorderColor = MaterialTheme.colorScheme.outline
                    ), modifier = Modifier.semantics { contentDescription = "Automatic monitoring" })
            }
        }
        if (!notifications) Button(onClick = onPermission, modifier = Modifier.fillMaxWidth()) { Text("Allow notifications") }
        Button(onClick = onTest, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(16.dp)) {
            Text("Send Test Notification", fontWeight = FontWeight.SemiBold)
        }
        if (message.isNotEmpty()) Text(message, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        Text("ON DEVICE · OFFLINE · PRIVATE", modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 8.dp),
            style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThresholdPanel(threshold: Int, onThreshold: (Int) -> Unit) {
    var draft by remember(threshold) { mutableIntStateOf(threshold) }
    Panel {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Alert threshold", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("$draft%", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold, modifier = Modifier.testTag("threshold_value"))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { draft = (draft - 1).coerceAtLeast(1); onThreshold(draft) }, enabled = draft > 1) {
                Text("−", fontSize = 24.sp, modifier = Modifier.semantics { contentDescription = "Decrease alert threshold" })
            }
            Slider(
                value = draft.toFloat(), onValueChange = { draft = it.roundToInt() },
                onValueChangeFinished = { onThreshold(draft) }, valueRange = 1f..100f, steps = 98,
                modifier = Modifier.weight(1f).testTag("threshold_slider").semantics { contentDescription = "Battery alert threshold" },
                thumb = { Box(Modifier.size(22.dp).background(MaterialTheme.colorScheme.primary, CircleShape)
                    .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape)) },
                track = {
                    Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.outlineVariant)) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth((draft - 1) / 99f).background(MaterialTheme.colorScheme.primary))
                    }
                }
            )
            IconButton(onClick = { draft = (draft + 1).coerceAtMost(100); onThreshold(draft) }, enabled = draft < 100) {
                Text("+", fontSize = 24.sp, modifier = Modifier.semantics { contentDescription = "Increase alert threshold" })
            }
        }
        Text("One alert per session. Unplug to reset.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ScreenHeader(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp), modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).testTag("back")) {
            Text("‹", fontSize = 32.sp, modifier = Modifier.semantics { contentDescription = "Back" })
        }
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun AppearanceScreen(selected: Appearance, onSelect: (Appearance) -> Unit, onBack: () -> Unit) {
    ScreenColumn {
        ScreenHeader("Appearance", onBack)
        Text("Choose your glow", style = MaterialTheme.typography.titleMedium)
        Text("Two dark palettes. The same quiet guardian.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Appearance.entries.forEach { appearance ->
                val checked = selected == appearance
                Card(
                    modifier = Modifier.fillMaxWidth().testTag(appearance.key)
                        .border(if (checked) 2.dp else 1.dp, if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
                        .selectable(selected = checked, role = Role.RadioButton, onClick = { onSelect(appearance) }),
                    shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface)
                ) {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(Modifier.size(44.dp).background(Color(appearance.background), CircleShape).border(3.dp, Color(appearance.accent), CircleShape))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(appearance.label, fontWeight = FontWeight.SemiBold)
                            Text(if (appearance == Appearance.ELECTRIC_NEON) "Electric lime · default" else "Soft mint · cool tones", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        RadioButton(selected = checked, onClick = null)
                    }
                }
            }
        }
        Text("Applied immediately and saved on your device.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun HelpScreen(onNotificationSettings: () -> Unit, onBack: () -> Unit) {
    ScreenColumn {
        ScreenHeader("Help", onBack)
        HelpSection("Getting started", "Allow notifications, then enable Automatic monitoring while Charge Guardian is open. The quiet ongoing notification indicates that the foreground service is monitoring battery and charger events. You can close this screen. One threshold alert is sent per observed charging session; unplug to reset it.")
        HelpSection("Connect your Amazfit Active 2", "In Zepp, open your Amazfit Active 2 → Notifications and reminders → App alerts. Grant Zepp notification access, then enable Charge Guardian. If it is missing from the list, send a test notification first. Keep the watch connected and Zepp allowed to run in the background. Check Zepp’s screen-off-only notification option.")
        HelpSection("Xiaomi / HyperOS", "Enable Background autostart for Charge Guardian and Zepp. Set battery management to No restrictions for both. Menu names vary by HyperOS version. If supported, lock both apps in recent apps. These settings improve reliability but cannot guarantee that HyperOS keeps an app running.")
        HelpSection("No alert on your phone?", "Check Android notification permission and the Battery alerts channel. Do Not Disturb, silent mode and channel settings can suppress sound or vibration. After force-stop, Android’s Active apps stop, or if monitoring disappears, reopen Charge Guardian and enable monitoring again. Boot recovery is attempted after unlock only when monitoring was previously enabled.")
        TextButton(onClick = onNotificationSettings) { Text("Open notification settings") }
        HelpSection("No alert on your watch?", "Send Test Notification and confirm it appears on the phone first. Then check Zepp app alerts, notification access, background permissions, Bluetooth connection and watch DND settings. Zepp controls forwarded notification appearance: the full-color launcher artwork and app-selected accent cannot be guaranteed on the watch. Charge Guardian uses standard Android notifications, with no direct Bluetooth connection or separate watch app.")
        HelpSection("Recovery and session limits", "If the app could not run through an entire unplug/replug cycle, Android’s current battery snapshot cannot reconstruct that missed session. Charge Guardian conservatively keeps the earlier alert claim to avoid duplicates. Changing the threshold never rearms a session that has already alerted. A process crash between saving the claim and posting the notification can lose that alert.")
        Text("Charge Guardian ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) · No network access", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HelpSection(title: String, body: String) {
    Panel {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
