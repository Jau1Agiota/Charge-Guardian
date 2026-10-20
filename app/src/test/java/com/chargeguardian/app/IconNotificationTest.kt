package com.chargeguardian.app

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class IconNotificationTest {
    private val app get() = ApplicationProvider.getApplicationContext<Application>()

    @Test fun adaptiveLauncherHasArtworkBackgroundAndThemedMonochrome() {
        for (resource in listOf(R.mipmap.ic_launcher, R.mipmap.ic_launcher_round)) {
            val icon = app.getDrawable(resource) as AdaptiveIconDrawable
            assertNotNull(icon.foreground)
            assertNotNull(icon.background)
            assertNotNull(icon.monochrome)
            val bitmap = Bitmap.createBitmap(192, 192, Bitmap.Config.ARGB_8888)
            icon.setBounds(0, 0, 192, 192)
            icon.draw(Canvas(bitmap))
            assertTrue(android.graphics.Color.alpha(bitmap.getPixel(96, 96)) > 0)
            save(bitmap, "launcher-$resource")
        }
    }

    @Test fun notificationBoltIsWhiteTransparentAndLegibleAtSmallSizes() {
        for (size in listOf(18, 24, 32)) {
            val icon = requireNotNull(app.getDrawable(R.drawable.ic_notification))
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            icon.setBounds(0, 0, size, size)
            icon.draw(Canvas(bitmap))
            var visible = 0
            for (y in 0 until size) for (x in 0 until size) {
                val pixel = bitmap.getPixel(x, y)
                if (android.graphics.Color.alpha(pixel) > 0) {
                    visible++
                    assertEquals(0xFFFFFF, pixel and 0xFFFFFF)
                }
            }
            assertTrue("Recognizable silhouette at ${size}px", visible > size * size * 0.08)
            assertTrue("No opaque notification square at ${size}px", visible < size * size * 0.45)
            assertEquals(0, android.graphics.Color.alpha(bitmap.getPixel(0, 0)))
            save(bitmap, "notification-${size}px")
        }
    }

    @Test fun newAlertsUseThePersistedThemeAndArtworkWithoutChangingChannels() = runBlocking {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val store = Store(app)
        val alerts = Alerts(app)
        val manager = app.getSystemService(NotificationManager::class.java)
        for (appearance in Appearance.entries) {
            store.appearance(appearance)
            for (test in listOf(false, true)) {
                assertTrue(alerts.send(96, test))
                val notification = shadowOf(manager).getNotification(if (test) 3 else 2)
                assertEquals(R.drawable.ic_notification, notification.smallIcon.resId)
                assertEquals(appearance.accent.toInt(), notification.color)
                assertEquals("battery_alerts", notification.channelId)
                assertTrue(notification.extras.getString(Notification.EXTRA_TITLE)!!.contains("Charge Guardian"))
                assertEquals("Battery reached 96%. Time to unplug your charger!", notification.extras.getString(Notification.EXTRA_TEXT))
            }
        }
    }

    private fun save(bitmap: Bitmap, name: String) {
        val file = File("build/outputs/icon-validation/$name.png")
        file.parentFile!!.mkdirs()
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
