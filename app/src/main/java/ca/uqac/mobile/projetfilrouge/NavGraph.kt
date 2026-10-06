package ca.uqac.mobile.projetfilrouge

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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

@Composable
fun NavGraph() {
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
                            popUpTo(navController.graph.findStartDestination().id) {
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
            startDestination = NavTab.Alarms.route,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            composable("login") {
                LoginScreen(onSignUp = {
                    navController.navigate("sign")
                },
                    onLogin = {
                        navController.navigate(NavTab.Alarms.route)
                    })
            }
            composable("sign") {
                SignScreen()
            }
            composable("alarmEdit") {
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
                ContactsScreen()
            }
            composable(NavTab.Profile.route) {
                AccountScreen()
            }
            composable("addContact"){
                AddContactScreen()
            }
            composable("voteRoom"){
                VoteRoomScreen()
            }
        }
    }
}
