package com.ridetrack.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Secondary metrics the rider can choose to show on the live ride screen. */
enum class LiveMetric(val label: String) {
    ACCELERATION("Acceleration"),
    BRAKING("Braking"),
    G_FORCE("G-force"),
    MAX_LEAN("Max lean"),
    HEADING("Heading"),
    ALTITUDE("Altitude"),
    ;

    companion object {
        const val MAX_VISIBLE = 4
        val DEFAULT = setOf(ACCELERATION, G_FORCE)
    }
}

data class Settings(
    val autoPause: Boolean = true,
    val demoMode: Boolean = false,
    val liveMetrics: Set<LiveMetric> = LiveMetric.DEFAULT,
    val showGForceIndicator: Boolean = true,
    val selectedBikeId: String? = null,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val autoPause = booleanPreferencesKey("auto_pause")
        val demoMode = booleanPreferencesKey("demo_mode")
        val liveMetrics = stringSetPreferencesKey("live_metrics")
        val gIndicator = booleanPreferencesKey("g_indicator")
        val selectedBike = stringPreferencesKey("selected_bike")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        Settings(
            autoPause = p[Keys.autoPause] ?: true,
            demoMode = p[Keys.demoMode] ?: false,
            liveMetrics = p[Keys.liveMetrics]
                ?.mapNotNull { name -> LiveMetric.entries.firstOrNull { it.name == name } }
                ?.toSet()
                ?: LiveMetric.DEFAULT,
            showGForceIndicator = p[Keys.gIndicator] ?: true,
            selectedBikeId = p[Keys.selectedBike],
        )
    }

    suspend fun setAutoPause(enabled: Boolean) = context.dataStore.edit { it[Keys.autoPause] = enabled }
    suspend fun setDemoMode(enabled: Boolean) = context.dataStore.edit { it[Keys.demoMode] = enabled }
    suspend fun setGForceIndicator(enabled: Boolean) = context.dataStore.edit { it[Keys.gIndicator] = enabled }
    suspend fun setSelectedBike(id: String) = context.dataStore.edit { it[Keys.selectedBike] = id }
    suspend fun setLiveMetrics(metrics: Set<LiveMetric>) =
        context.dataStore.edit { it[Keys.liveMetrics] = metrics.map { m -> m.name }.toSet() }
}
