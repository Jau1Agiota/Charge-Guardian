package com.chargeguardian.app

import kotlinx.coroutines.flow.first
import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class Alerts(private val context: Context) {
    private val manager = context.getSystemService(NotificationManager::class.java)
    init {
        manager.createNotificationChannel(NotificationChannel("battery_alerts", "Battery alerts", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Battery threshold and test alerts forwarded through Zepp"; enableVibration(true)
        })
        manager.createNotificationChannel(NotificationChannel("monitoring", "Monitoring status", NotificationManager.IMPORTANCE_LOW))
    }
    fun allowed(): Boolean = (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
        NotificationManagerCompat.from(context).areNotificationsEnabled() && manager.getNotificationChannel("battery_alerts").importance != NotificationManager.IMPORTANCE_NONE
    private fun open(): PendingIntent = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    fun ongoing(): Notification = NotificationCompat.Builder(context, "monitoring")
        .setSmallIcon(R.drawable.ic_notification).setContentTitle("Charge Guardian is monitoring")
        .setContentText("Waiting for battery and charger changes").setContentIntent(open())
        .setCategory(NotificationCompat.CATEGORY_SERVICE).setOngoing(true).setSilent(true).build()
    suspend fun send(threshold: Int, test: Boolean = false): Boolean {
        if (!allowed()) return false
        val appearance = Store(context).settings.first().appearance
        return try {
            manager.notify(if (test) 3 else 2, NotificationCompat.Builder(context, "battery_alerts")
                .setSmallIcon(R.drawable.ic_notification).setContentTitle(if (test) "Charge Guardian • Test" else "Charge Guardian • Battery alert")
                .setContentText("Battery reached $threshold%. Time to unplug your charger!")
                .setStyle(NotificationCompat.BigTextStyle().bigText("Battery reached $threshold%. Time to unplug your charger!"))
                .setColor(appearance.accent.toInt())
                .setContentIntent(open()).setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).build())
            true
        } catch (_: SecurityException) { false }
    }
}
