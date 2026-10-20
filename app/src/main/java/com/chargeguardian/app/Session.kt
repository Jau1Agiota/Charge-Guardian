package com.chargeguardian.app

/** Pure, durable session transition. A threshold change never rearms an alerted session. */
data class Session(val connected: Boolean = false, val alerted: Boolean = false, val id: Long = 0)
data class Transition(val session: Session, val alert: Boolean)
fun transition(previous: Session, connected: Boolean, percentage: Int, threshold: Int): Transition {
    if (!connected) return Transition(previous.copy(connected = false, alerted = false), false)
    val current = if (previous.connected) previous else Session(true, false, previous.id + 1)
    val alert = !current.alerted && percentage in threshold..100
    return Transition(current.copy(alerted = current.alerted || alert), alert)
}
