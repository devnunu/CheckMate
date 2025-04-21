package co.kr.checkmate.presentation

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import co.kr.checkmate.presentation.home.HomeScreen
import co.kr.checkmate.ui.ext.ScreenAnim
import co.kr.checkmate.ui.ext.animComposable
import co.kr.checkmate.ui.navigation.NavRoute
import org.koin.androidx.compose.getViewModel

@Composable
fun MainScreen() {
    val activity = LocalContext.current as ComponentActivity
    val navController = rememberNavController()

    BackHandler {
        if (!navController.popBackStack()) {
            activity.finish()
        }
    }
    NavHost(
        navController = navController,
        startDestination = NavRoute.Home
    ) {
        animComposable<NavRoute.Home>(
            screenAnim = ScreenAnim.FADE_IN_OUT
        ) {
            HomeScreen(
                viewModel = getViewModel(),
            )
        }
    }
}