package ca.uqac.mobile.projetfilrouge

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraph
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ca.uqac.mobile.projetfilrouge.ui.screen.AlarmEditScreen
import ca.uqac.mobile.projetfilrouge.ui.screen.AlarmsScreen
import ca.uqac.mobile.projetfilrouge.ui.screen.LoginScreen
import ca.uqac.mobile.projetfilrouge.ui.screen.SignScreen

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "alarmEdit"
    ) {
        composable("login") {
            LoginScreen(onSignUp = {
                navController.navigate("sign")
            })
        }
        composable("sign") {
            SignScreen()
        }
        composable ( "alarmEdit" ){
            AlarmEditScreen()
        }
    }
}