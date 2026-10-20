package com.chargeguardian.app

import android.content.*
import android.os.BatteryManager

data class Battery(val percentage: Int = -1, val connected: Boolean = false)
fun Context.battery(): Battery = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)).toBattery()
fun Intent?.toBattery(): Battery {
    if (this == null) return Battery()
    val level = getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    return Battery(if (level >= 0 && scale > 0) (level * 100 / scale).coerceIn(0, 100) else -1,
        getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0)
}
