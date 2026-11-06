package ca.uqac.mobile.projetfilrouge.data

import ca.uqac.mobile.projetfilrouge.BuildConfig
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

val supabase = createSupabaseClient(
    supabaseUrl = BuildConfig.SUPABASE_URL,
    supabaseKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY,
) {
    install(Auth)
    install(Postgrest)
    install(Realtime)
    install(Storage)
}

val currentUid: String? get() = supabase.auth.currentUserOrNull()?.id

val uidChanges: Flow<String?> = supabase.auth.sessionStatus
    .filter { it !is SessionStatus.Initializing }
    .map { currentUid }
    .distinctUntilChanged()

// Relance fetch à chaque changement de la table (Realtime)
fun <T> observeTable(table: String, fetch: suspend () -> T): Flow<T> = channelFlow {
    val channel = supabase.channel("$table-${UUID.randomUUID()}")
    val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") { this.table = table }
    launch { changes.collect { send(fetch()) } }
    channel.subscribe(blockUntilSubscribed = true)
    send(fetch())
    try {
        awaitCancellation()
    } finally {
        withContext(NonCancellable) { supabase.realtime.removeChannel(channel) }
    }
}
