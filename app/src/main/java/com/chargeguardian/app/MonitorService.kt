package com.chargeguardian.app

import android.app.Service
import android.content.*
import android.os.IBinder
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MonitorService : Service() {
    companion object {
        private val active = MutableStateFlow(false)
        val running = active.asStateFlow()
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val samples = Channel<Battery>(Channel.UNLIMITED)
    private val store by lazy { Store(this) }
    private val alerts by lazy { Alerts(this) }
    private var registered = false
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val current = if (intent.action == Intent.ACTION_BATTERY_CHANGED) intent.toBattery() else context.battery()
            samples.trySend(when (intent.action) {
                Intent.ACTION_POWER_DISCONNECTED -> current.copy(connected = false)
                Intent.ACTION_POWER_CONNECTED -> current.copy(connected = true)
                else -> current
            })
        }
    }
    override fun onCreate() {
        super.onCreate()
        try { startForeground(1, alerts.ongoing()) }
        catch (_: IllegalStateException) { stopSelf(); return }
        catch (_: SecurityException) { stopSelf(); return }
        active.value = true
        registerReceiver(receiver, IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED); addAction(Intent.ACTION_POWER_CONNECTED); addAction(Intent.ACTION_POWER_DISCONNECTED)
        })
        registered = true
        scope.launch {
            for (sample in samples) {
                // Leave the session unclaimed when notifications cannot be delivered.
                if (!sample.connected || alerts.allowed()) store.sample(sample.connected, sample.percentage)?.let { alerts.send(it) }
            }
        }
        scope.launch {
            store.settings.map { it.enabled to it.threshold }.distinctUntilChanged().collect { (enabled, _) ->
                if (!enabled) { stopForeground(STOP_FOREGROUND_REMOVE); stopSelf() }
                else samples.send(battery())
            }
        }
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        samples.trySend(battery())
        return START_STICKY
    }
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() {
        if (registered) unregisterReceiver(receiver)
        active.value = false
        samples.close(); scope.cancel(); super.onDestroy()
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in listOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED)) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (Store(context).settings.first().enabled) {
                    try { context.startForegroundService(Intent(context, MonitorService::class.java)) }
                    catch (_: IllegalStateException) { /* OS/OEM blocked restart; reopen the UI. */ }
                    catch (_: SecurityException) { /* Permission or platform restriction; reopen UI. */ }
                }
            } finally { pending.finish() }
        }
    }
}
