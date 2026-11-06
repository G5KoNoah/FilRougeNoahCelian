package ca.uqac.mobile.projetfilrouge.data

import android.content.Context
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.util.UUID

// Upload des sonneries et cache local
object RingtoneRepository {

    const val MAX_SIZE_BYTES = 5 * 1024 * 1024

    private const val BUCKET = "ringtones"
    private const val CACHE_DIR = "ringtones"

    private val extensions = mapOf(
        "audio/mpeg" to "mp3",
        "audio/mp3" to "mp3",
        "audio/wav" to "wav",
        "audio/x-wav" to "wav",
        "audio/wave" to "wav",
        "audio/vnd.wave" to "wav",
    )

    val acceptedMimeTypes: Array<String> get() = extensions.keys.toTypedArray()

    // Extension pour ce type MIME, null s'il n'est pas supporté
    fun extensionFor(mimeType: String?): String? = extensions[mimeType?.lowercase()]

    // Envoie la sonnerie dans le bucket et renvoie son URL publique
    suspend fun upload(ownerUid: String, bytes: ByteArray, mimeType: String): String {
        val extension = extensionFor(mimeType) ?: error("Unsupported audio type: $mimeType")
        val path = "$ownerUid/${UUID.randomUUID()}.$extension"
        val bucket = supabase.storage.from(BUCKET)
        bucket.upload(path, bytes) {
            contentType = ContentType.parse(mimeType)
        }
        return bucket.publicUrl(path)
    }

    // Fichier du cache local pour cette URL
    fun localFile(context: Context, url: String): File {
        val extension = url.substringBefore('?').substringAfterLast('.', "mp3")
        val name = "${url.hashCode().toUInt()}.$extension"
        return File(File(context.filesDir, CACHE_DIR), name)
    }

    // Télécharge la sonnerie dans le cache si elle n'y est pas
    suspend fun download(context: Context, url: String) = withContext(Dispatchers.IO) {
        val target = localFile(context, url)
        if (target.exists()) return@withContext
        target.parentFile?.mkdirs()
        val temp = File(target.parentFile, "${target.name}.part")
        URL(url).openStream().use { input -> temp.outputStream().use { input.copyTo(it) } }
        temp.renameTo(target)
    }

    // Supprime du cache les sonneries qui ne sont plus utilisées
    fun keepOnly(context: Context, urls: Collection<String>) {
        val keep = urls.map { localFile(context, it).name }.toSet()
        File(context.filesDir, CACHE_DIR).listFiles()
            ?.filter { it.name !in keep }
            ?.forEach { it.delete() }
    }
}
