package ca.uqac.mobile.projetfilrouge

import androidx.compose.foundation.Image
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ca.uqac.mobile.projetfilrouge.ui.screen.WelcomeScreen
import ca.uqac.mobile.projetfilrouge.ui.theme.ProjetFilRougeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("Create","OnCreate")
        enableEdgeToEdge()
        setContent {
            ProjetFilRougeTheme {
                WelcomeScreen()
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
        WelcomeScreen()
    }
}