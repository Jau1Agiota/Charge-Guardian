package com.chargeguardian.app

import android.graphics.Canvas
import androidx.compose.ui.semantics.SemanticsActions
import java.io.File
import android.graphics.Bitmap
import android.content.Intent
import android.os.BatteryManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w393dp-h852dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppearanceUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val store: Store get() = Store(ApplicationProvider.getApplicationContext<android.content.Context>())

    @Before fun resetPreferences() {
        runBlocking { store.appearance(Appearance.ELECTRIC_NEON); store.threshold(95) }
        compose.activity.sendBroadcast(Intent(Intent.ACTION_BATTERY_CHANGED)
            .putExtra(BatteryManager.EXTRA_LEVEL, 73).putExtra(BatteryManager.EXTRA_SCALE, 100)
            .putExtra(BatteryManager.EXTRA_PLUGGED, BatteryManager.BATTERY_PLUGGED_USB))
        compose.waitForIdle()
    }

    @Test fun themeSelectionSurvivesActivityRecreationAndKeepsBackNavigation() {
        captureScreen("electric-neon")
        compose.onNodeWithTag("open_appearance").performClick()
        compose.onNodeWithTag("electric_neon").assertIsSelected()
        compose.onNodeWithTag("neon_mint").performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodes(hasTestTag("neon_mint") and isSelected()).fetchSemanticsNodes().size == 1
        }
        runBlocking { withTimeout(10_000) { store.settings.first { it.appearance == Appearance.NEON_MINT } } }
        captureScreen("appearance-neon-mint")
        // A new Store wrapper reads the actual persisted preference rather than UI draft state.
        assertEquals(Appearance.NEON_MINT, runBlocking { Store(compose.activity).settings.first().appearance })
        compose.activityRule.scenario.recreate()
        compose.waitUntil(10_000) {
            compose.onAllNodes(hasTestTag("neon_mint") and isSelected()).fetchSemanticsNodes().size == 1
        }
        compose.onNodeWithTag("neon_mint").assertIsSelected()
        compose.onNodeWithTag("back").performClick()
        compose.onNodeWithTag("battery_percentage").assertIsDisplayed()
        captureScreen("neon-mint")
        compose.onNodeWithTag("open_appearance").performClick()
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag("open_help").assertIsDisplayed()
    }

    @Test fun helpIsSeparateScrollableAndReturnsToDashboard() {
        compose.onNodeWithText("Connect your Amazfit Active 2").assertDoesNotExist()
        compose.onNodeWithTag("open_help").performClick()
        captureScreen("help-electric-neon")
        compose.onNodeWithText("Xiaomi / HyperOS").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Recovery and session limits").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("back").performScrollTo().performClick()
        compose.onNodeWithTag("battery_percentage").assertIsDisplayed()
        compose.onNodeWithText("Recovery and session limits").assertDoesNotExist()
    }

    private fun captureScreen(name: String) {
        compose.activity.sendBroadcast(Intent(Intent.ACTION_BATTERY_CHANGED)
            .putExtra(BatteryManager.EXTRA_LEVEL, 73).putExtra(BatteryManager.EXTRA_SCALE, 100)
            .putExtra(BatteryManager.EXTRA_PLUGGED, BatteryManager.BATTERY_PLUGGED_USB))
        compose.runOnIdle {
            val view = compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            val file = File("build/outputs/ui-validation/$name.png")
            file.parentFile!!.mkdirs()
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test fun everyThresholdFrom80Through100PersistsExactly() {
        for (value in 80..100) {
            compose.onNodeWithTag("threshold_slider").performScrollTo()
                .performSemanticsAction(SemanticsActions.SetProgress) { it(value.toFloat()) }
            compose.waitUntil(10_000) {
                compose.onAllNodes(hasTestTag("threshold_value") and hasText("$value%")).fetchSemanticsNodes().size == 1 &&
                    runBlocking { store.settings.first().threshold == value }
            }
            compose.onNodeWithTag("threshold_value").assertTextEquals("$value%")
        }
        compose.activityRule.scenario.recreate()
        compose.waitUntil(10_000) {
            compose.onAllNodes(hasTestTag("threshold_value") and hasText("100%")).fetchSemanticsNodes().size == 1
        }
        compose.onNodeWithTag("threshold_value").assertTextEquals("100%")
    }
}
