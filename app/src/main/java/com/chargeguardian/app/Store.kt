package com.chargeguardian.app

import androidx.datastore.core.DataStore
import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("guardian")
data class Settings(val threshold: Int = 95, val enabled: Boolean = false, val session: Session = Session(), val appearance: Appearance = Appearance.ELECTRIC_NEON)
class Store internal constructor(private val db: DataStore<Preferences>) {
    constructor(context: Context) : this(context.applicationContext.dataStore)
    private val threshold = intPreferencesKey("threshold")
    private val enabled = booleanPreferencesKey("enabled")
    private val appearance = stringPreferencesKey("appearance")
    private val connected = booleanPreferencesKey("connected")
    private val alerted = booleanPreferencesKey("alerted")
    private val sessionId = longPreferencesKey("session_id")
    val settings = db.data.map { p -> Settings(p[threshold] ?: 95, p[enabled] ?: false,
        Session(p[connected] ?: false, p[alerted] ?: false, p[sessionId] ?: 0), Appearance.fromKey(p[appearance])) }
    suspend fun appearance(value: Appearance) { db.edit { it[appearance] = value.key } }
    suspend fun threshold(value: Int) { db.edit { it[threshold] = value.coerceIn(1, 100) } }
    suspend fun enable(value: Boolean) { db.edit { it[enabled] = value } }
    // Atomic durable claim before notification: at most one attempt, even after process death.
    suspend fun sample(charging: Boolean, percentage: Int): Int? {
        var result: Int? = null
        db.edit { p ->
            if (p[enabled] != true) return@edit
            val limit = p[threshold] ?: 95
            val next = transition(Session(p[connected] ?: false, p[alerted] ?: false, p[sessionId] ?: 0), charging, percentage, limit)
            p[connected] = next.session.connected; p[alerted] = next.session.alerted; p[sessionId] = next.session.id
            if (next.alert) result = limit
        }
        return result
    }
}
