package ca.uqac.mobile.projetfilrouge

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import ca.uqac.mobile.projetfilrouge.data.RingtoneRepository
import ca.uqac.mobile.projetfilrouge.receiver.AlarmReceiver

class AlarmSoundService : Service() {

    private var player: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private val autoStop = Runnable { stopSelf() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val title = intent?.getStringExtra(EXTRA_TITLE)
        ServiceCompat.startForeground(
            this,
            AlarmReceiver.NOTIFICATION_ID,
            AlarmReceiver.buildNotification(this, title),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0,
        )

        play(intent?.getStringExtra(EXTRA_RINGTONE_URL))
        handler.removeCallbacks(autoStop)
        handler.postDelayed(autoStop, MAX_RING_MS)
        return START_NOT_STICKY
    }

    private fun play(ringtoneUrl: String?) {
        releasePlayer()
        val candidates = buildList {
            if (ringtoneUrl != null) {
                val local = RingtoneRepository.localFile(this@AlarmSoundService, ringtoneUrl)
                if (local.exists()) add(Uri.fromFile(local))
                add(Uri.parse(ringtoneUrl))
            }
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)?.let(::add)
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)?.let(::add)
        }
        player = candidates.firstNotNullOfOrNull { uri -> runCatching { startPlayer(uri) }.getOrNull() }
    }

    private fun startPlayer(uri: Uri): MediaPlayer {
        val mediaPlayer = MediaPlayer()
        try {
            mediaPlayer.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            mediaPlayer.setDataSource(this, uri)
            mediaPlayer.isLooping = true
            mediaPlayer.prepare()
            mediaPlayer.start()
            return mediaPlayer
        } catch (e: Exception) {
            mediaPlayer.release()
            throw e
        }
    }

    private fun releasePlayer() {
        player?.run {
            runCatching { stop() }
            release()
        }
        player = null
    }

    override fun onDestroy() {
        handler.removeCallbacks(autoStop)
        releasePlayer()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_RINGTONE_URL = "ringtone_url"
        private const val MAX_RING_MS = 5 * 60_000L

        fun start(context: Context, title: String?, ringtoneUrl: String?) {
            val intent = Intent(context, AlarmSoundService::class.java)
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_RINGTONE_URL, ringtoneUrl)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AlarmSoundService::class.java))
        }
    }
}
