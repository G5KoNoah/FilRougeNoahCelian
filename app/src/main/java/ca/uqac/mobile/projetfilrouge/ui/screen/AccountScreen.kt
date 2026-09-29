package ca.uqac.mobile.projetfilrouge.ui.screen


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.uqac.mobile.projetfilrouge.R
@Composable
fun AccountScreen() {
    Scaffold(modifier = Modifier.fillMaxSize(), containerColor = Color.DarkGray) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            MainInfo()
            OtherInfo()
            SettingsInfo()
        }
    }
}

@Composable
fun Modifier.cardStyle(): Modifier {
    val forme = RoundedCornerShape(16.dp)
    return this
        .fillMaxWidth(0.75f)
        .background(color = Color.Gray, shape = forme)
        .border(width = 2.dp, color = Color.Black, shape = forme)
        .padding(24.dp)
}

@Composable
fun MainInfo(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.cardStyle(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.uqac_logo),
            contentDescription = "Logo UQAC",
            modifier = Modifier.size(120.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("FirstName", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("SurName", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text("UserID", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
    }
}

@Composable
fun OtherInfo(modifier: Modifier = Modifier) {
    Column(modifier = modifier.cardStyle()) {
        Text("Contact", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            //Icon(Icons.Default.Email, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(12.dp))
            Text("Mail", color = Color.White)
        }
    }
}

@Composable
fun SettingsInfo(modifier: Modifier = Modifier) {
    var darkTheme by remember { mutableStateOf(true) }

    Column(modifier = modifier.cardStyle()) {
        Text("Settings", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Theme :", color = Color.White)
            Switch(
                checked = darkTheme,
                onCheckedChange = { darkTheme = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color.Black
                )
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black,
                contentColor = Color.White
            )
        ) {
            Text("Logout")
        }
    }
}