package ca.uqac.mobile.projetfilrouge

import android.content.Context
import ca.uqac.mobile.projetfilrouge.data.AlarmRepository
import ca.uqac.mobile.projetfilrouge.data.RingtoneRepository
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

// Synchronise les alarmes Supabase de l'utilisateur avec l'AlarmManager
class AlarmSync(private val context: Context) {

    private val scheduler = AlarmScheduler(context)
    private val prefs = context.getSharedPreferences("scheduled_alarms", Context.MODE_PRIVATE)
    private val serializer = ListSerializer(SharedAlarm.serializer())

    // Écoute les alarmes de l'utilisateur connecté et les programme
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
                .collect { alarms ->
                    apply(alarms)
                    cacheRingtones(alarms)
                }
        }
    }

    // Télécharge les sonneries utilisées et supprime les autres
    private suspend fun cacheRingtones(alarms: List<SharedAlarm>) {
        val urls = alarms.mapNotNull { it.ringtoneUrl }.distinct()
        urls.forEach { url -> runCatching { RingtoneRepository.download(context, url) } }
        RingtoneRepository.keepOnly(context, urls)
    }

    // Reprogramme les alarmes sauvegardées, après un redémarrage
    fun rescheduleSaved() {
        saved().forEach { scheduler.schedule(it) }
    }

    // Programme les alarmes actives, annule les retirées et les sauvegarde
    private fun apply(alarms: List<SharedAlarm>) {
        val activeIds = alarms.map { it.id }.toSet()
        saved().filter { it.id !in activeIds }.forEach { scheduler.cancel(it) }
        alarms.forEach { scheduler.schedule(it) }
        prefs.edit().putString(KEY, Json.encodeToString(serializer, alarms)).apply()
    }

    // Alarmes sauvegardées localement
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
