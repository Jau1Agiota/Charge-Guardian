package com.chargeguardian.app

import android.app.Application
import android.content.*
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GuardianViewModel(app: Application) : AndroidViewModel(app) {
    private val store = Store(app)
    val settings = store.settings.stateIn(viewModelScope, SharingStarted.Eagerly, Settings())
    private val _battery: MutableStateFlow<Battery> = MutableStateFlow(app.battery())
    val battery: StateFlow<Battery> = _battery.asStateFlow()
    val message = MutableStateFlow("")
    val running = MonitorService.running
    val notifications = MutableStateFlow(false)
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) { _battery.value = intent.toBattery() }
    }
    init { app.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED)); refresh() }
    fun refresh() {
        _battery.value = getApplication<Application>().battery()
        notifications.value = Alerts(getApplication()).allowed()
    }
    fun appearance(value: Appearance) { viewModelScope.launch { store.appearance(value) } }
    fun threshold(value: Int) { viewModelScope.launch { store.threshold(value) } }
    fun monitoring(enable: Boolean) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            if (enable && !Alerts(app).allowed()) { message.value = "Allow notifications and the Battery alerts channel first."; return@launch }
            store.enable(enable)
            if (enable) {
                try { app.startForegroundService(Intent(app, MonitorService::class.java)); message.value = "Monitoring enabled. Keep the monitoring notification active." }
                catch (_: IllegalStateException) { message.value = "Android blocked startup. Reopen the app and try again." }
                catch (_: SecurityException) { message.value = "Android denied monitoring startup. Check app permissions." }
            } else { app.stopService(Intent(app, MonitorService::class.java)); message.value = "Monitoring paused." }
            refresh()
        }
    }
    fun test() { viewModelScope.launch {
        message.value = if (Alerts(getApplication()).send(settings.value.threshold, true)) "Test notification sent. Check your phone and watch." else "Notifications are blocked. Enable them in Android settings."
        refresh()
    } }
    override fun onCleared() { getApplication<Application>().unregisterReceiver(receiver) }
}
