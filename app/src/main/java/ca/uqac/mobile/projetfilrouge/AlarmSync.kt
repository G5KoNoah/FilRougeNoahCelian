package ca.uqac.mobile.projetfilrouge

import android.content.Context
import ca.uqac.mobile.projetfilrouge.data.AlarmRepository
import ca.uqac.mobile.projetfilrouge.data.SharedAlarm
import ca.uqac.mobile.projetfilrouge.data.uidChanges
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class AlarmSync(context: Context) {

    private val scheduler = AlarmScheduler(context)
    private val prefs = context.getSharedPreferences("scheduled_alarms", Context.MODE_PRIVATE)
    private val serializer = ListSerializer(SharedAlarm.serializer())

    @OptIn(ExperimentalCoroutinesApi::class)
    fun start(scope: CoroutineScope) {
        scope.launch {
            uidChanges
                .flatMapLatest { uid ->
                    if (uid == null) {
                        flowOf(emptyList())
                    } else {
                        AlarmRepository.observeAlarmsOf(uid)
                            .map { alarms -> alarms.filter { it.isEnabledFor(uid) } }
                            .catch { }
                    }
                }
                .collect { apply(it) }
        }
    }

    fun rescheduleSaved() {
        saved().forEach { scheduler.schedule(it) }
    }

    private fun apply(alarms: List<SharedAlarm>) {
        val activeIds = alarms.map { it.id }.toSet()
        saved().filter { it.id !in activeIds }.forEach { scheduler.cancel(it) }
        alarms.forEach { scheduler.schedule(it) }
        prefs.edit().putString(KEY, Json.encodeToString(serializer, alarms)).apply()
    }

    private fun saved(): List<SharedAlarm> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return try {
            Json.decodeFromString(serializer, raw)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private companion object {
        const val KEY = "alarms"
    }
}
