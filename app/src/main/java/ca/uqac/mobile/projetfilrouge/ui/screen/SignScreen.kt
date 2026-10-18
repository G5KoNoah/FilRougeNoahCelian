package ca.uqac.mobile.projetfilrouge.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.sp
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@Composable
fun SignScreen (){
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding), horizontalAlignment = Alignment.CenterHorizontally) {
            Logo(modifier = Modifier.padding(top = 200.dp), verticalArrangement = Arrangement.Center)
            FormSign(modifier = Modifier.padding(top = 100.dp))
        }
    }
}


@Composable
fun FormSign(modifier: Modifier = Modifier) {
    var emailText by remember { mutableStateOf("") }
    var passwordText by remember { mutableStateOf("") }
    var confirmPasswordText by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var estErreur by remember { mutableStateOf(false) }
    var enCours by remember { mutableStateOf(false) }
    val forme = RoundedCornerShape(16.dp)

    val auth = Firebase.auth
    Column(
        modifier = modifier
            .fillMaxWidth(0.75f)
            .border(width = 2.dp, color = Color.Black, shape = forme)
            .background(color = MaterialTheme.colorScheme.surface, shape = forme)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Create your account",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(24.dp))

        TextField(
            value = emailText,
            onValueChange = { emailText = it },
            placeholder = { Text("Login") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = passwordText,
            onValueChange = { passwordText = it },
            placeholder = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))

        TextField(
            value = confirmPasswordText,
            onValueChange = { confirmPasswordText = it },
            placeholder = { Text("Confirm Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                // Vérifications avant d'appeler Firebase
                val erreur = when {
                    emailText.isBlank() -> "Entre ton adresse mail"
                    passwordText.length < 6 -> "Le mot de passe doit faire au moins 6 caractères"
                    passwordText != confirmPasswordText -> "Les mots de passe ne correspondent pas"
                    else -> null
                }
                if (erreur != null) {
                    message = erreur
                    estErreur = true
                    return@Button
                }

                enCours = true
                message = null
                auth.createUserWithEmailAndPassword(emailText.trim(), passwordText)
                    .addOnSuccessListener {
                        // Compte créé avec succès
                        enCours = false
                        estErreur = false
                        message = "Compte créé avec succès"
                    }
                    .addOnFailureListener { e ->
                        // Erreur (email déjà utilisé, mail invalide...)
                        enCours = false
                        estErreur = true
                        message = e.localizedMessage ?: "Erreur lors de la création du compte"
                    }
            },
            enabled = !enCours,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black,
            )
        ) {
            Text(
                text = "Sign up",
                modifier = Modifier.padding(top = 10.dp, bottom = 10.dp),
                fontSize = 20.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        message?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = it,
                color = if (estErreur) Color.Red else Color.Green,
                fontWeight = FontWeight.Bold
            )
        }
    }
}