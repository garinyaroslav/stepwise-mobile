package com.github.stepwise.ui.compose.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.github.stepwise.ui.compose.profile.ProfileScreen
import com.github.stepwise.ui.compose.student.StudentProjectDetailScreen
import com.github.stepwise.ui.compose.student.StudentProjectsScreen

sealed class StudentScreen(val route: String, val title: String, val icon: ImageVector) {
    object Projects : StudentScreen("projects", "Проекты", Icons.Filled.Work)
    object Profile : StudentScreen("profile/{userId}", "Профиль", Icons.Filled.Person) {
        fun createRoute(userId: Long) = "profile/$userId"
    }
    object ProjectDetail : StudentScreen("project_detail/{workId}", "Подробнее", Icons.Filled.Work) {
        fun createRoute(workId: Long) = "project_detail/$workId"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentApp() {
    val navController = rememberNavController()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Студент") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            StudentBottomNavigation(navController)
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = StudentScreen.Projects.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(StudentScreen.Projects.route) {
                StudentProjectsScreen(
                    onNavigateToDetail = { workId ->
                        navController.navigate(StudentScreen.ProjectDetail.createRoute(workId))
                    }
                )
            }
            
            composable(
                route = StudentScreen.Profile.route,
                arguments = listOf(navArgument("userId") { type = NavType.LongType })
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getLong("userId") ?: -1L
                ProfileScreen(userId = userId)
            }
            
            composable(
                route = StudentScreen.ProjectDetail.route,
                arguments = listOf(navArgument("workId") { type = NavType.LongType })
            ) { backStackEntry ->
                val workId = backStackEntry.arguments?.getLong("workId") ?: -1L
                StudentProjectDetailScreen(workId = workId)
            }
        }
    }
}

@Composable
fun StudentBottomNavigation(navController: NavHostController) {
    val items = listOf(
        StudentScreen.Projects,
        StudentScreen.Profile
    )
    
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        
        items.forEach { screen ->
            val isProfileScreen = screen is StudentScreen.Profile
            val route = if (isProfileScreen) "profile/{userId}" else screen.route
            
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.title) },
                label = { Text(screen.title) },
                selected = currentDestination?.hierarchy?.any { 
                    it.route == route || (isProfileScreen && it.route?.startsWith("profile/") == true)
                } == true,
                onClick = {
                    val navigateRoute = if (isProfileScreen) {
                        StudentScreen.Profile.createRoute(-1L)
                    } else {
                        screen.route
                    }
                    
                    navController.navigate(navigateRoute) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
