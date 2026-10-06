package com.example.ui.navigation

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.repo.AuthRepository
import com.example.data.repo.ChatRepository
import com.example.data.repo.DailyPlanRepository
import com.example.data.repo.TaskRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.AuthViewModel
import com.example.ui.chat.ChatScreen
import com.example.ui.chat.ChatViewModel
import com.example.ui.notes.NotesScreen
import com.example.ui.tasks.TasksScreen
import com.example.ui.tasks.TasksViewModel
import com.example.ui.today.TodayScreen
import com.example.ui.today.TodayViewModel

enum class NavDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val testTag: String
) {
    TODAY("today", "Today", Icons.Outlined.Today, "tab_today"),
    CHAT("chat", "Chat", Icons.Outlined.ChatBubbleOutline, "tab_chat"),
    TASKS("tasks", "Tasks", Icons.Outlined.CheckCircleOutline, "tab_tasks"),
    NOTES("notes", "Notes", Icons.Outlined.Description, "tab_notes")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NavigationShell(
    authRepository: AuthRepository,
    taskRepository: TaskRepository,
    dailyPlanRepository: DailyPlanRepository,
    chatRepository: ChatRepository,
    noteRepository: com.example.data.repo.NoteRepository,
    modifier: Modifier = Modifier
) {
    val sessionState by authRepository.sessionState.collectAsStateWithLifecycle()

    // If user is not authenticated and has not entered as Guest, show AuthScreen
    if (sessionState == null) {
        val authViewModel: AuthViewModel = viewModel(
            factory = AuthViewModel.Factory(authRepository)
        )
        AuthScreen(
            viewModel = authViewModel,
            modifier = modifier
        )
        return
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: NavDestination.TODAY.route
    val isImeVisible = WindowInsets.isImeVisible

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.statusBars,
        bottomBar = {
            if (!isImeVisible) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    NavDestination.entries.forEach { destination ->
                        val selected = currentRoute == destination.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != destination.route) {
                                    navController.navigate(destination.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = destination.label
                                )
                            },
                            label = {
                                Text(
                                    text = destination.label,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag(destination.testTag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavDestination.TODAY.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(NavDestination.TODAY.route) {
                val todayViewModel: TodayViewModel = viewModel(
                    factory = TodayViewModel.Factory(taskRepository, dailyPlanRepository)
                )
                TodayScreen(
                    viewModel = todayViewModel,
                    userSession = sessionState,
                    onSignOut = { authRepository.logout() },
                    onNavigateToTasks = {
                        navController.navigate(NavDestination.TASKS.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToChat = {
                        navController.navigate(NavDestination.CHAT.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable(NavDestination.CHAT.route) {
                val chatViewModel: ChatViewModel = viewModel(
                    factory = ChatViewModel.provideFactory(chatRepository)
                )
                ChatScreen(viewModel = chatViewModel)
            }

            composable(NavDestination.TASKS.route) {
                val tasksViewModel: TasksViewModel = viewModel(
                    factory = TasksViewModel.provideFactory(taskRepository)
                )
                TasksScreen(viewModel = tasksViewModel)
            }

            composable(NavDestination.NOTES.route) {
                val notesViewModel: com.example.ui.notes.NotesViewModel = viewModel(
                    factory = com.example.ui.notes.NotesViewModel.Factory(noteRepository)
                )
                NotesScreen(viewModel = notesViewModel)
            }
        }
    }
}
