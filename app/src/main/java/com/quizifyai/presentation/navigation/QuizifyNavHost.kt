package com.quizifyai.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.quizifyai.presentation.screens.HomeScreen
import com.quizifyai.presentation.screens.LoginScreen
import com.quizifyai.presentation.screens.QuizDetailScreen
import com.quizifyai.presentation.screens.RegisterScreen
import com.quizifyai.presentation.screens.SplashScreen
import com.quizifyai.presentation.viewmodel.AuthViewModel
import com.quizifyai.presentation.viewmodel.HomeViewModel
import com.quizifyai.presentation.viewmodel.QuizDetailViewModel
import com.quizifyai.utils.AppContainer

@Composable
fun QuizifyNavHost(appContainer: AppContainer) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel(factory = appContainer.authViewModelFactory())
    val authState = authViewModel.uiState.collectAsStateWithLifecycle().value

    LaunchedEffect(authState.isCheckingAuth, authState.currentUser) {
        if (authState.isCheckingAuth) return@LaunchedEffect

        val currentRoute = navController.currentDestination?.route
        val destination = if (authState.currentUser == null) Routes.LOGIN else Routes.HOME
        val shouldMoveToHome = destination == Routes.HOME &&
            currentRoute in setOf(Routes.SPLASH, Routes.LOGIN, Routes.REGISTER)
        val shouldMoveToLogin = destination == Routes.LOGIN && currentRoute != Routes.LOGIN

        if (shouldMoveToHome || shouldMoveToLogin) {
            navController.navigate(destination) {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
    ) {
        composable(Routes.SPLASH) {
            SplashScreen()
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                uiState = authState,
                onLogin = authViewModel::login,
                onRegisterClick = {
                    authViewModel.clearError()
                    navController.navigate(Routes.REGISTER)
                },
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                uiState = authState,
                onRegister = authViewModel::register,
                onLoginClick = {
                    authViewModel.clearError()
                    navController.popBackStack()
                },
            )
        }

        composable(Routes.HOME) {
            val homeViewModel: HomeViewModel = viewModel(factory = appContainer.homeViewModelFactory())
            val homeState = homeViewModel.uiState.collectAsStateWithLifecycle().value

            HomeScreen(
                uiState = homeState,
                onPdfSelected = homeViewModel::onPdfSelected,
                onQuizClick = { quizId -> navController.navigate(Routes.quizDetail(quizId)) },
                onLogoutClick = authViewModel::logout,
                onMessageShown = homeViewModel::clearMessage,
                onErrorShown = homeViewModel::clearError,
            )
        }

        composable(
            route = Routes.QUIZ_DETAIL_ROUTE,
            arguments = listOf(navArgument("quizId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val quizId = backStackEntry.arguments?.getString("quizId").orEmpty()
            val viewModel: QuizDetailViewModel = viewModel(factory = appContainer.quizDetailViewModelFactory())
            val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

            LaunchedEffect(quizId) {
                viewModel.loadQuiz(quizId)
            }

            QuizDetailScreen(
                uiState = uiState,
                onBackClick = { navController.popBackStack() },
            )
        }
    }
}
