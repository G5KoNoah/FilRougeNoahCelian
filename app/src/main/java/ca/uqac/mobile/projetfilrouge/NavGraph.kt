package ca.uqac.mobile.projetfilrouge

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraph
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ca.uqac.mobile.projetfilrouge.ui.screen.AccountScreen
import ca.uqac.mobile.projetfilrouge.ui.screen.AlarmEditScreen
import ca.uqac.mobile.projetfilrouge.ui.screen.AlarmsScreen
import ca.uqac.mobile.projetfilrouge.ui.screen.LoginScreen
import ca.uqac.mobile.projetfilrouge.ui.screen.SignScreen
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@Composable
fun NavGraph() {
    val auth = Firebase.auth
    val utilisateurActuel = auth.currentUser
    var identifiant = ""
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") {
            if (utilisateurActuel != null) {
                identifiant = utilisateurActuel.uid
                navController.navigate("account")
            } else {
                LoginScreen(onSignUp = {
                    navController.navigate("sign")
                })
            }

        }
        composable("sign") {
            SignScreen()
        }
        composable ( "alarmEdit" ){
            AlarmEditScreen()
        }
        composable ( "account" ){
            AccountScreen()
        }
    }
}