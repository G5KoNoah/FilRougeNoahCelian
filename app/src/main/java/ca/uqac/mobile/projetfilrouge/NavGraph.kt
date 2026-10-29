package ca.uqac.mobile.projetfilrouge

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ca.uqac.mobile.projetfilrouge.ui.components.BottomNavBar
import ca.uqac.mobile.projetfilrouge.ui.components.NavTab
import ca.uqac.mobile.projetfilrouge.ui.screen.AccountScreen
import ca.uqac.mobile.projetfilrouge.ui.screen.AddContactScreen
import ca.uqac.mobile.projetfilrouge.ui.screen.AlarmEditScreen
import ca.uqac.mobile.projetfilrouge.ui.screen.AlarmsScreen
import ca.uqac.mobile.projetfilrouge.ui.screen.ContactsScreen
import ca.uqac.mobile.projetfilrouge.ui.screen.LoginScreen
import ca.uqac.mobile.projetfilrouge.ui.screen.SignScreen
import ca.uqac.mobile.projetfilrouge.ui.screen.VoteRoomScreen
import ca.uqac.mobile.projetfilrouge.data.currentUid

@Composable
fun NavGraph() {
    // Déjà connecté -> on démarre directement sur les alarmes. Calculé une seule fois.
    val startDestination = remember {
        if (currentUid != null) NavTab.Alarms.route else "login"
    }
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentTab = NavTab.entries.find { it.route == backStackEntry?.destination?.route }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (currentTab != null) {
                BottomNavBar(
                    selectedTab = currentTab,
                    onTabSelected = { tab ->
                        navController.navigate(tab.route) {
                            popUpTo(NavTab.Alarms.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            composable("login") {
                LoginScreen(onSignUp = {
                    navController.navigate("sign")
                },
                    onLogin = {
                        navController.navigate(NavTab.Alarms.route) {
                            popUpTo("login") { inclusive = true }
                        }
                    })
            }
            composable("sign") {
                SignScreen(onLogin = {
                    navController.navigate("login")
                })
            }
            composable(
                "alarmEdit?alarmId={alarmId}",
                arguments = listOf(navArgument("alarmId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }),
            ) {
                AlarmEditScreen( onBackScreen = {
                    navController.popBackStack()
                })
            }
            composable(NavTab.Alarms.route) {
                AlarmsScreen( onAlarmEdit = {
                    navController.navigate("alarmEdit")
            })
            }
            composable(NavTab.Contacts.route) {
                ContactsScreen(onAddContact = {
                    navController.navigate("addContact")
                })
            }
            composable(NavTab.Profile.route) {
                AccountScreen(onLogout = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                })
            }
            composable("addContact"){
                AddContactScreen(onBack = {
                    navController.popBackStack()
                })
            }
            composable("voteRoom"){
                VoteRoomScreen()
            }
        }
    }
}
