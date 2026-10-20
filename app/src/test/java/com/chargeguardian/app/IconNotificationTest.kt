package com.chargeguardian.app

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Icon
import android.os.Parcel
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.*
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

    @Test fun launcherLayersAndCommonMasksKeepArtworkProminentAndCriticalDetailsSafe() {
        val icon = app.getDrawable(R.mipmap.ic_launcher) as AdaptiveIconDrawable
        val foreground = Bitmap.createBitmap(216, 216, Bitmap.Config.ARGB_8888)
        icon.foreground.setBounds(0, 0, 216, 216)
        icon.foreground.draw(Canvas(foreground))
        assertEquals(0, android.graphics.Color.alpha(foreground.getPixel(0, 0)))
        assertEquals(255, android.graphics.Color.alpha(foreground.getPixel(108, 108)))
        save(foreground, "foreground-layer")
        val background = Bitmap.createBitmap(216, 216, Bitmap.Config.ARGB_8888)
        icon.background.setBounds(0, 0, 216, 216)
        icon.background.draw(Canvas(background))
        assertEquals(android.graphics.Color.rgb(9, 12, 9), background.getPixel(0, 0))
        save(background, "background-layer")

        val visibleColumns = (0 until 216).filter { x ->
            (0 until 216).any { y -> android.graphics.Color.alpha(foreground.getPixel(x, y)) >= 128 }
        }
        val artDp = (visibleColumns.last() - visibleColumns.first() + 1) * 108f / 216f
        assertTrue("Prominent foreground without excessive inset", artDp in 71f..73f)
        // Essential facial/battery details belong in the 66dp safe area.
        // The complete framed scene is full-bleed; its background corners need not fit there.
        val safeLandmarks = listOf(230f to 450f, 680f to 520f, 1015f to 270f,
            1140f to 515f, 740f to 895f, 110f to 430f)
        safeLandmarks.forEach { (x, y) ->
            val dx = (x / 1254f - .5f) * artDp
            val dy = (y / 1254f - .5f) * artDp + 2f
            assertTrue("Essential detail ($x,$y) inside 66dp safe area", hypot(dx, dy) <= 33f)
        }
        val hairContour = listOf(600f to 0f, 800f to 40f, 70f to 430f)
        hairContour.forEach { (x, y) ->
            val dx = (x / 1254f - .5f) * artDp
            val dy = (y / 1254f - .5f) * artDp + 2f
            assertTrue("Main hair contour ($x,$y) survives circular mask", hypot(dx, dy) < 36f)
        }
        for (size in listOf(48, 144)) for (mask in listOf("circle", "rounded-square", "squircle")) {
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val path = Path()
            when (mask) {
                "circle" -> path.addCircle(size / 2f, size / 2f, size / 2f, Path.Direction.CW)
                "rounded-square" -> path.addRoundRect(RectF(0f, 0f, size.toFloat(), size.toFloat()), size * .22f, size * .22f, Path.Direction.CW)
                else -> {
                    for (degree in 0..360) {
                        val angle = Math.toRadians(degree.toDouble())
                        val x = (size / 2.0 * (1 + sign(cos(angle)) * sqrt(abs(cos(angle))))).toFloat()
                        val y = (size / 2.0 * (1 + sign(sin(angle)) * sqrt(abs(sin(angle))))).toFloat()
                        if (degree == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    path.close()
                }
            }
            canvas.clipPath(path)
            // 108dp layers extend 18dp beyond each edge of the central 72dp viewport.
            val offset = size / 4
            for (layer in listOf(icon.background, icon.foreground)) {
                layer.setBounds(-offset, -offset, size + offset, size + offset)
                layer.draw(canvas)
            }
            assertEquals(255, android.graphics.Color.alpha(bitmap.getPixel(size / 2, size / 2)))
            save(bitmap, "launcher-$mask-${size}px")
        }
    }

    @Test fun notificationBatteryAndBoltIsWhiteTransparentAndLegibleAtSmallSizes() {
        for (size in listOf(18, 24, 32)) {
            val icon = requireNotNull(app.getDrawable(R.drawable.ic_notification))
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            icon.setBounds(0, 0, size, size)
            icon.draw(Canvas(bitmap))
            var alphaCoverage = 0L
            for (y in 0 until size) for (x in 0 until size) {
                val pixel = bitmap.getPixel(x, y)
                if (android.graphics.Color.alpha(pixel) > 0) {
                    alphaCoverage += android.graphics.Color.alpha(pixel)
                    assertEquals(0xFFFFFF, pixel and 0xFFFFFF)
                }
            }
            assertTrue("Recognizable silhouette at ${size}px", alphaCoverage > size * size * 255 * 0.20)
            assertTrue("No opaque notification square at ${size}px", alphaCoverage < size * size * 255 * 0.45)
            assertEquals(0, android.graphics.Color.alpha(bitmap.getPixel(0, 0)))
            // Measure alpha area, not AA fringe count; verify a real hollow battery, not a filled box.
            assertEquals(0, android.graphics.Color.alpha(bitmap.getPixel(size * 7 / 24, size * 7 / 24)))
            assertEquals(255, android.graphics.Color.alpha(bitmap.getPixel(size * 5 / 24, size * 10 / 24)))
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
                assertEquals(Notification.CATEGORY_REMINDER, notification.category)
                assertEquals(NotificationManager.IMPORTANCE_HIGH, manager.getNotificationChannel(notification.channelId).importance)
                assertTrue(notification.extras.getString(Notification.EXTRA_TITLE)!!.contains("Charge Guardian"))
                assertEquals("Battery reached 96%. Time to unplug your charger!", notification.extras.getString(Notification.EXTRA_TEXT))
            }
        }
    }

    @Test fun monitoringHasItsOwnQuietChannelAndValidSmallIcon() {
        val alerts = Alerts(app)
        val notification = alerts.ongoing()
        assertEquals(R.drawable.ic_notification, notification.smallIcon.resId)
        assertEquals("monitoring", notification.channelId)
        assertEquals(Notification.CATEGORY_SERVICE, notification.category)
        assertTrue(notification.flags and Notification.FLAG_ONGOING_EVENT != 0)
        assertEquals(NotificationManager.IMPORTANCE_LOW, app.getSystemService(NotificationManager::class.java)
            .getNotificationChannel(notification.channelId).importance)
        assertEquals("Charge Guardian", app.applicationInfo.loadLabel(app.packageManager).toString())
    }

    @Test fun allNotificationIconsRemainResolvableAfterAndroidParcelSerialization() = runBlocking {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val alerts = Alerts(app)
        val manager = app.getSystemService(NotificationManager::class.java)
        val notifications = mutableListOf(alerts.ongoing())
        for (test in listOf(false, true)) {
            assertTrue(alerts.send(95, test))
            notifications += shadowOf(manager).getNotification(if (test) 3 else 2)
        }
        for ((index, original) in notifications.withIndex()) {
            val parcel = Parcel.obtain()
            try {
                original.writeToParcel(parcel, 0)
                parcel.setDataPosition(0)
                val forwarded = Notification.CREATOR.createFromParcel(parcel)
                assertEquals(Icon.TYPE_RESOURCE, forwarded.smallIcon.type)
                assertEquals(app.packageName, forwarded.smallIcon.resPackage)
                assertEquals(R.drawable.ic_notification, forwarded.smallIcon.resId)
                assertNotNull(forwarded.smallIcon.loadDrawable(app))
                assertEquals(Notification.VISIBILITY_PRIVATE, forwarded.visibility)
                assertEquals(if (index == 0) Notification.PRIORITY_DEFAULT else Notification.PRIORITY_HIGH, forwarded.priority)
                assertEquals(original.channelId, forwarded.channelId)
                assertEquals(original.category, forwarded.category)
                assertEquals(original.extras.getString(Notification.EXTRA_TITLE), forwarded.extras.getString(Notification.EXTRA_TITLE))
                assertEquals(original.extras.getString(Notification.EXTRA_TEXT), forwarded.extras.getString(Notification.EXTRA_TEXT))
            } finally { parcel.recycle() }
        }
    }

    private fun save(bitmap: Bitmap, name: String) {
        val file = File("build/outputs/icon-validation/$name.png")
        file.parentFile!!.mkdirs()
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
