package com.chargeguardian.app

import org.junit.Assert.*
import org.junit.Test

class SessionTest {
    @Test fun thresholdDetection() {
        val below = transition(Session(), true, 94, 95)
        assertFalse(below.alert)
        assertTrue(transition(below.session, true, 95, 95).alert)
    }
    @Test fun duplicatesAreSuppressed() {
        val first = transition(Session(), true, 95, 95)
        assertTrue(first.alert)
        assertFalse(transition(first.session, true, 99, 95).alert)
        assertFalse(transition(first.session, true, 94, 95).alert)
    }
    @Test fun disconnectResetsSession() {
        val first = transition(Session(), true, 95, 95)
        val unplugged = transition(first.session, false, 96, 95)
        assertFalse(unplugged.session.alerted)
        assertFalse(unplugged.alert)
        val second = transition(unplugged.session, true, 96, 95)
        assertTrue(second.alert)
        assertEquals(first.session.id + 1, second.session.id)
    }
    @Test fun connectionAboveThresholdAlerts() { assertTrue(transition(Session(), true, 100, 95).alert) }
    @Test fun persistedProcessStatePreventsDuplicate() {
        val saved = transition(Session(), true, 95, 95).session
        val restored = Session(saved.connected, saved.alerted, saved.id)
        assertFalse(transition(restored, true, 96, 95).alert)
    }
    @Test fun recoveryBeforeThresholdStillAlerts() { assertTrue(transition(Session(true, false, 8), true, 96, 95).alert) }
    @Test fun recoveryWhileDisconnectedClearsClaim() {
        assertEquals(Session(false, false, 8), transition(Session(true, true, 8), false, 50, 95).session)
    }
    @Test fun loweringThresholdDuringChargingAlerts() { assertTrue(transition(Session(true, false, 1), true, 90, 85).alert) }
    @Test fun raisingThresholdWaits() { assertFalse(transition(Session(true, false, 1), true, 95, 99).alert) }
    @Test fun thresholdChangesNeverRearmClaimedSession() { assertFalse(transition(Session(true, true, 1), true, 99, 98).alert) }
    @Test fun unknownBatteryDoesNotAlert() { assertFalse(transition(Session(), true, -1, 95).alert) }
    @Test fun disconnectedBatteryNeverAlerts() { assertFalse(transition(Session(), false, 100, 95).alert) }
}
