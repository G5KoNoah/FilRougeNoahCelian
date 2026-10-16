package ca.uqac.mobile.projetfilrouge

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ca.uqac.mobile.projetfilrouge.ui.screen.AccountScreen
import ca.uqac.mobile.projetfilrouge.ui.screen.AlarmEditScreen
import ca.uqac.mobile.projetfilrouge.ui.screen.AlarmsScreen
import ca.uqac.mobile.projetfilrouge.ui.screen.LoginScreen
import ca.uqac.mobile.projetfilrouge.ui.screen.SignScreen
import ca.uqac.mobile.projetfilrouge.ui.screen.VoteRoomScreen
import ca.uqac.mobile.projetfilrouge.ui.theme.ProjetFilRougeTheme
import ca.uqac.mobile.projetfilrouge.viewModel.SettingsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("Create","OnCreate")
        enableEdgeToEdge()
        setContent {
            // Même instance que celle de SettingsInfo (même activité) : le thème suit le switch
            val settingsViewModel: SettingsViewModel = viewModel()
            val estSombre by settingsViewModel.estSombre.collectAsStateWithLifecycle()
            ProjetFilRougeTheme(darkTheme = estSombre) {
               //LoginScreen()
                //SignScreen()
                //AccountScreen()
                //AlarmsScreen()
                //AlarmEditScreen()
                //VoteRoomScreen()
                NavGraph()
            }
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