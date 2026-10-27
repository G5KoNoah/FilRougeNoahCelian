package ca.uqac.mobile.projetfilrouge.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.sp
import ca.uqac.mobile.projetfilrouge.data.AccountData
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

@Composable
fun SignScreen(onLogin: () -> Unit) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            Logo(modifier = Modifier.heightIn(max = 160.dp), verticalArrangement = Arrangement.Center)
            FormSign(modifier = Modifier.widthIn(max = 480.dp), onLogin)
        }
    }
}


@Composable
fun FormSign(modifier: Modifier = Modifier, onLogin: () -> Unit) {
    var emailText by rememberSaveable() { mutableStateOf("") }
    var passwordText by rememberSaveable { mutableStateOf("") }
    var confirmPasswordText by rememberSaveable { mutableStateOf("") }
    var firstNameText by rememberSaveable() { mutableStateOf("") }
    var lastNameText by rememberSaveable() { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    var isError by rememberSaveable { mutableStateOf(false) }
    var isUsing by rememberSaveable { mutableStateOf(false) }
    val forme = RoundedCornerShape(16.dp)

    val auth = Firebase.auth
    val db = Firebase.firestore
    Column(
        modifier = modifier
            .fillMaxWidth()
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

        TextField(
            value = firstNameText,
            onValueChange = { firstNameText = it },
            placeholder = { Text("First name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))

        TextField(
            value = lastNameText,
            onValueChange = { lastNameText = it },
            placeholder = { Text("Last name") },
            singleLine = true,
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
                    isError = true
                    return@Button
                }

                isUsing = true
                message = null
                auth.createUserWithEmailAndPassword(emailText.trim(), passwordText)
                    .addOnSuccessListener { result ->
                        val uid = result.user?.uid ?: return@addOnSuccessListener
                        val account = AccountData(emailText.trim(), firstNameText.trim(), lastNameText.trim())
                        db.collection("Account").document(uid).set(account)
                            .addOnSuccessListener {
                                isUsing = false
                                isError = false
                                message = "Compte créé avec succès"
                                onLogin()
                            }
                            .addOnFailureListener { e ->
                                isUsing = false
                                isError = true
                                message = e.localizedMessage ?: "Erreur Firestore"
                            }
                    }
                    .addOnFailureListener { e ->
                        // Erreur (email déjà utilisé, mail invalide...)
                        isUsing = false
                        isError = true
                        message = e.localizedMessage ?: "Erreur lors de la création du compte"
                    }
            },
            enabled = !isUsing,
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
                color = if (isError) Color.Red else Color.Green,
                fontWeight = FontWeight.Bold
            )
        }
    }
}