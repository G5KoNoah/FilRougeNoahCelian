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

// URL et clé lues dans local.properties (voir app/build.gradle.kts).
// La clé "publishable" est faite pour être embarquée dans l'app : la sécurité passe par les règles RLS (supabase/schema.sql)
val supabase = createSupabaseClient(
    supabaseUrl = BuildConfig.SUPABASE_URL,
    supabaseKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY,
) {
    install(Auth)
    install(Postgrest)
    install(Realtime)
}

val currentUid: String? get() = supabase.auth.currentUserOrNull()?.id

// Équivalent de l'AuthStateListener Firebase : émet l'uid à chaque connexion / déconnexion
val uidChanges: Flow<String?> = supabase.auth.sessionStatus
    .filter { it !is SessionStatus.Initializing }
    .map { currentUid }
    .distinctUntilChanged()

// Équivalent d'un addSnapshotListener : relance la requête à chaque changement de la table.
// Les règles RLS limitent les événements reçus aux lignes que l'utilisateur a le droit de lire.
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
