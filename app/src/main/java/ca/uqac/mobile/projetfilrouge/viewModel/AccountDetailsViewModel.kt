package ca.uqac.mobile.projetfilrouge.viewModel

import ca.uqac.mobile.projetfilrouge.R
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.lifecycle.viewModelScope
import ca.uqac.mobile.projetfilrouge.data.AccountData
import ca.uqac.mobile.projetfilrouge.data.currentUid
import ca.uqac.mobile.projetfilrouge.data.observeTable
import ca.uqac.mobile.projetfilrouge.data.supabase
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max

class AccountDetailsViewModel(application: Application) : AndroidViewModel(application) {

    // Les ViewModels n'ont pas de stringResource : on passe par l'Application, qui suit la langue de l'appareil
    private fun str(@StringRes id: Int): String = getApplication<Application>().getString(id)

    val uid: String? = currentUid

    private val _account = MutableStateFlow<AccountData?>(null)
    val account: StateFlow<AccountData?> = _account.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isUploadingAvatar = MutableStateFlow(false)
    val isUploadingAvatar: StateFlow<Boolean> = _isUploadingAvatar.asStateFlow()

    init {
        if (uid == null) {
            _error.value = str(R.string.error_no_user)
        } else {
            // L'écoute s'arrête toute seule quand le ViewModel est détruit (viewModelScope)
            viewModelScope.launch {
                observeTable("accounts") {
                    supabase.from("accounts").select { filter { eq("id", uid) } }
                        .decodeSingleOrNull<AccountData>()
                }
                    .catch { e -> _error.value = e.localizedMessage ?: str(R.string.error_server) }
                    .collect { account ->
                        if (account == null) {
                            _error.value = str(R.string.account_not_found)
                        } else {
                            _error.value = null
                            _account.value = account
                        }
                    }
            }
        }
    }

    fun uploadAvatar(uri: Uri) {
        val userId = uid ?: return
        if (_isUploadingAvatar.value) return
        _isUploadingAvatar.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) { compressAvatar(uri) }
                val path = "$userId/avatar.jpg"
                val bucket = supabase.storage.from(AVATAR_BUCKET)
                bucket.upload(path, bytes) {
                    upsert = true
                    contentType = ContentType.Image.JPEG
                }
                val url = "${bucket.publicUrl(path)}?v=${System.currentTimeMillis()}"
                supabase.from("accounts").update({ set("avatar_url", url) }) {
                    filter { eq("id", userId) }
                }
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: str(R.string.avatar_error)
            } finally {
                _isUploadingAvatar.value = false
            }
        }
    }

    // On vide avatar_url d'abord : c'est ce qui fait revenir les initiales.
    // Si le fichier ne peut pas être supprimé, il sera écrasé au prochain upload.
    fun removeAvatar() {
        val userId = uid ?: return
        if (_isUploadingAvatar.value) return
        _isUploadingAvatar.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                supabase.from("accounts").update({ set("avatar_url", null as String?) }) {
                    filter { eq("id", userId) }
                }
                runCatching { supabase.storage.from(AVATAR_BUCKET).delete("$userId/avatar.jpg") }
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: str(R.string.avatar_error)
            } finally {
                _isUploadingAvatar.value = false
            }
        }
    }

    private fun compressAvatar(uri: Uri): ByteArray {
        val bitmap = decodeScaled(uri) ?: error(str(R.string.avatar_error))
        return ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
            output.toByteArray()
        }
    }

    private fun decodeScaled(uri: Uri): Bitmap? {
        val resolver = getApplication<Application>().contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(resolver, uri)
            return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val scale = AVATAR_SIZE.toFloat() / max(info.size.width, info.size.height)
                if (scale < 1f) {
                    decoder.setTargetSize((info.size.width * scale).toInt(), (info.size.height * scale).toInt())
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sampleSize = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= AVATAR_SIZE) sampleSize *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }

    private companion object {
        const val AVATAR_BUCKET = "avatars"
        const val AVATAR_SIZE = 512
        const val JPEG_QUALITY = 85
    }
}
