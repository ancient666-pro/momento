package com.momento.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.momento.core.designsystem.theme.MainBackground
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.momento.core.designsystem.components.MomentoBottomBar
import com.momento.core.navigation.Routes
import com.momento.feature.auth.AuthScreen
import com.momento.feature.create.CreateScreen
import com.momento.feature.discover.DiscoverScreen
import com.momento.feature.home.HomeScreen
import com.momento.feature.pod.PodScreen
import com.momento.feature.profile.ProfileScreen

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Routes.HOME

    Scaffold(
        containerColor = MainBackground,
        bottomBar = {
            MomentoBottomBar(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    if (route != currentRoute) {
                        navController.navigate(route) {
                            popUpTo(Routes.HOME) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(bottom = innerPadding.calculateBottomPadding())) {
            NavHost(
                navController = navController,
                startDestination = Routes.HOME
            ) {
                composable(Routes.HOME) { HomeScreen() }
                composable(Routes.DISCOVER) { DiscoverScreen() }
                composable(Routes.CREATE) { CreateScreen() }
                composable(Routes.POD) { PodScreen() }
                composable(Routes.PROFILE) { ProfileScreen() }
                composable(Routes.AUTH) { AuthScreen() }
            }
        }
    }
}
