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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("Create","OnCreate")
        enableEdgeToEdge()
        stopVibrationIfFromAlarm(intent)
        AlarmSync(applicationContext).start(lifecycleScope)
        setContent {
            // Même instance que celle de SettingsInfo (même activité) : le thème suit le switch
            val settingsViewModel: SettingsViewModel = viewModel()
            val estSombre by settingsViewModel.estSombre.collectAsStateWithLifecycle()
            ProjetFilRougeTheme(darkTheme = estSombre) {
                NavGraph()
            }
        }
    }

    // L'app était déjà ouverte quand on a touché la notification
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        stopVibrationIfFromAlarm(intent)
    }

    // Ouverte depuis la notification d'alarme : on arrête la vibration lancée par AlarmReceiver
    private fun stopVibrationIfFromAlarm(intent: Intent?) {
        if (intent?.getBooleanExtra(AlarmReceiver.EXTRA_STOP_VIBRATION, false) == true) {
            AppVibrator(this).stop()
            intent.removeExtra(AlarmReceiver.EXTRA_STOP_VIBRATION)   // évite de rejouer ça après une rotation
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d("Start", "OnStart")
    }

    override fun onPause() {
        super.onPause()
        Log.d("Pause", "OnPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d("Stop", "OnStop")
    }

    override fun onResume() {
        super.onResume()
        Log.d("Resume", "OnResume")
    }

    override fun onRestart() {
        super.onRestart()
        Log.d("Restart", "OnRestart")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("Destroy", "OnDestroy")
    }
}



@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ProjetFilRougeTheme {
        //LoginScreen()
    }
}