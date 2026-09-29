package ca.uqac.mobile.projetfilrouge.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

@Composable
fun LoginScreen (){
    Scaffold(modifier = Modifier.fillMaxSize(), containerColor = Color.DarkGray) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding), horizontalAlignment = Alignment.CenterHorizontally) {
            Logo(modifier = Modifier.padding(top = 200.dp), verticalArrangement = Arrangement.Center)
            FormLogin(modifier = Modifier.padding(top = 100.dp))
        }
    }
}

@Composable
fun Logo(modifier: Modifier, verticalArrangement: Arrangement.Vertical) {
    Column(modifier = modifier, verticalArrangement = verticalArrangement) {
        Image(
            painter = painterResource(id = R.drawable.uqac_logo),
            contentDescription = "C'est l'école"
        )
    }


}

@Composable
fun FormLogin(modifier: Modifier = Modifier) {
    var emailText by remember { mutableStateOf("") }
    var passwordText by remember { mutableStateOf("") }
    val forme = RoundedCornerShape(16.dp)

    Column(
        modifier = modifier
            .fillMaxWidth(0.75f)
            .background(color = Color.Gray, shape = forme)
            .border(width = 2.dp, color = Color.Black, shape = forme)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Login to your account",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(24.dp))

        TextField(
            value = emailText,
            onValueChange = { emailText = it },
            placeholder = { Text("Login") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = passwordText,
            onValueChange = { passwordText = it },
            placeholder = { Text("Password") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black,
            )
        ) {
            Text(
                text = "Login",
                modifier = Modifier.padding(top = 10.dp, bottom = 10.dp),
                fontSize = 20.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "or sign in",
            color = Color.White,
            fontSize = 14.sp,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier
                .align(Alignment.End)
                .clickable { /* action au clic */ }
        )
    }
}