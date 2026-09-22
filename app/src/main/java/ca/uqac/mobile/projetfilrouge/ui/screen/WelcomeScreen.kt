package ca.uqac.mobile.projetfilrouge.ui.screen

import androidx.compose.foundation.Image
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ca.uqac.mobile.projetfilrouge.R
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Composable
fun WelcomeScreen (){
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding), horizontalAlignment = Alignment.CenterHorizontally) {
            Logo(modifier = Modifier, verticalArrangement = Arrangement.Center)
            Form(modifier = Modifier)
        }
    }
}

@Composable
fun Logo(modifier: Modifier, verticalArrangement: Arrangement.Vertical) {
    Column(modifier = modifier, verticalArrangement = verticalArrangement) {
        Image(
            painter = painterResource(id = R.drawable.uqac),
            contentDescription = "C'est l'école"
        )
    }


}

@Composable
fun Form(modifier: Modifier = Modifier){
    var text1 by remember() { mutableStateOf("") }
    var text2 by remember() { mutableStateOf("") }
    Column(modifier = modifier.background(color = Color.Gray).padding(top = 50.dp)) {
        Text("Login")
        TextField( value = text1, onValueChange = { text1 = it})
        Text("Password")
        TextField( value = text2, onValueChange = { text2 = it})
        Button(onClick = { }) {
            Text(text = "Cliquez-moi")
        }
    }
}