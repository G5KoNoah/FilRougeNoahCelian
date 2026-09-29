package ca.uqac.mobile.projetfilrouge

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ca.uqac.mobile.projetfilrouge.ui.screen.LoginScreen
import ca.uqac.mobile.projetfilrouge.ui.theme.ProjetFilRougeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("Create","OnCreate")
        enableEdgeToEdge()
        setContent {
            ProjetFilRougeTheme {
                LoginScreen()
                //SignScreen()
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
        LoginScreen()
    }
}