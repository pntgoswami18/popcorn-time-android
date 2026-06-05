package com.popcorntime.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.popcorntime.android.ui.movies.MovieBrowserScreen
import com.popcorntime.android.ui.movies.MovieDetailScreen
import com.popcorntime.android.ui.player.PlayerScreen

sealed class Screen(val route: String) {
    data object MovieBrowser : Screen("movies")
    data object MovieDetail : Screen("movies/{imdbId}") {
        fun createRoute(imdbId: String) = "movies/$imdbId"
    }
    data object Player : Screen("player/{imdbId}/{quality}") {
        fun createRoute(imdbId: String, quality: String) = "player/$imdbId/$quality"
    }
}

@Composable
fun PopcornNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.MovieBrowser.route) {

        composable(Screen.MovieBrowser.route) {
            MovieBrowserScreen(
                onMovieClick = { imdbId ->
                    navController.navigate(Screen.MovieDetail.createRoute(imdbId))
                }
            )
        }

        composable(
            route = Screen.MovieDetail.route,
            arguments = listOf(navArgument("imdbId") { type = NavType.StringType }),
        ) { backStack ->
            val imdbId = backStack.arguments!!.getString("imdbId")!!
            MovieDetailScreen(
                imdbId = imdbId,
                onBack = { navController.popBackStack() },
                onPlayClick = { quality ->
                    navController.navigate(Screen.Player.createRoute(imdbId, quality))
                },
            )
        }

        composable(
            route = Screen.Player.route,
            arguments = listOf(
                navArgument("imdbId") { type = NavType.StringType },
                navArgument("quality") { type = NavType.StringType },
            ),
        ) { backStack ->
            val imdbId = backStack.arguments!!.getString("imdbId")!!
            val quality = backStack.arguments!!.getString("quality")!!
            PlayerScreen(
                imdbId = imdbId,
                quality = quality,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
