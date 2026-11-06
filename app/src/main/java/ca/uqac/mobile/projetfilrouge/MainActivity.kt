package ca.uqac.mobile.projetfilrouge

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import ca.uqac.mobile.projetfilrouge.receiver.AlarmReceiver
import ca.uqac.mobile.projetfilrouge.ui.theme.ProjetFilRougeTheme
import ca.uqac.mobile.projetfilrouge.viewModel.SettingsViewModel

// Activité unique de l'app
class MainActivity : ComponentActivity() {
    // Lance la synchro des alarmes et affiche la navigation
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("Create","OnCreate")
        enableEdgeToEdge()
        stopVibrationIfFromAlarm(intent)
        AlarmSync(applicationContext).start(lifecycleScope)
        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val estSombre by settingsViewModel.estSombre.collectAsStateWithLifecycle()
            ProjetFilRougeTheme(darkTheme = estSombre) {
                NavGraph()
            }
        }
    }

    // Arrête l'alarme si l'app est rouverte depuis sa notification
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        stopVibrationIfFromAlarm(intent)
    }

    // Coupe le son et la vibration si l'intent vient de la notification d'alarme
    private fun stopVibrationIfFromAlarm(intent: Intent?) {
        if (intent?.getBooleanExtra(AlarmReceiver.EXTRA_STOP_VIBRATION, false) == true) {
            AppVibrator(this).stop()
            AlarmSoundService.stop(this)
            intent.removeExtra(AlarmReceiver.EXTRA_STOP_VIBRATION)
        }
    }

    // Log du cycle de vie
    override fun onStart() {
        super.onStart()
        Log.d("Start", "OnStart")
    }

    // Log du cycle de vie
    override fun onPause() {
        super.onPause()
        Log.d("Pause", "OnPause")
    }

    // Log du cycle de vie
    override fun onStop() {
        super.onStop()
        Log.d("Stop", "OnStop")
    }

    // Log du cycle de vie
    override fun onResume() {
        super.onResume()
        Log.d("Resume", "OnResume")
    }

    // Log du cycle de vie
    override fun onRestart() {
        super.onRestart()
        Log.d("Restart", "OnRestart")
    }

    // Log du cycle de vie
    override fun onDestroy() {
        super.onDestroy()
        Log.d("Destroy", "OnDestroy")
    }
}



// Aperçu vide
@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ProjetFilRougeTheme {
    }
}