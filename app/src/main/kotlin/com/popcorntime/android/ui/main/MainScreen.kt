package com.popcorntime.android.ui.main

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.popcorntime.android.domain.model.ContentType
import com.popcorntime.android.domain.model.LibraryContentType
import com.popcorntime.android.ui.library.LibraryScreen
import com.popcorntime.android.ui.movies.MovieBrowserScreen
import com.popcorntime.android.ui.movies.MovieDetailScreen
import com.popcorntime.android.ui.player.PlayerScreen
import com.popcorntime.android.ui.settings.AppearanceSettingsScreen
import com.popcorntime.android.ui.settings.RemoteSettingsScreen
import com.popcorntime.android.ui.settings.SourceSettingsScreen
import com.popcorntime.android.ui.settings.SubtitleSettingsScreen
import com.popcorntime.android.ui.settings.TorrentSettingsScreen
import com.popcorntime.android.ui.settings.SettingsScreen
import com.popcorntime.android.ui.settings.TraktSettingsScreen
import com.popcorntime.android.ui.shows.ShowBrowserScreen
import com.popcorntime.android.ui.shows.ShowDetailScreen

sealed class Tab(val route: String, val label: String, val icon: ImageVector) {
    data object Movies : Tab("tab_movies", "Movies", Icons.Default.Movie)
    data object Series : Tab("tab_series", "Series", Icons.Default.Slideshow)
    data object Anime : Tab("tab_anime", "Anime", Icons.Default.Star)
    data object Library : Tab("tab_library", "Library", Icons.Default.VideoLibrary)

    companion object {
        val all = listOf(Movies, Series, Anime, Library)
    }
}

@Composable
fun MainScreen(isTv: Boolean = false) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = navBackStackEntry?.destination

    // Hide bottom bar when inside the player or settings screens
    val showBottomBar = currentDest?.route?.let { route ->
        !route.startsWith("player/") &&
            !route.startsWith("player_local/") &&
            route != "settings" &&
            !route.startsWith("settings/")
    } != false

    if (isTv) {
        // TV: PermanentNavigationDrawer side nav — only when player is not shown
        val selectedTab = Tab.all.firstOrNull { tab ->
            currentDest?.hierarchy?.any { it.route == tab.route } == true
        } ?: Tab.Movies
        if (showBottomBar) {
            PermanentNavigationDrawer(
                drawerContent = {
                    PermanentDrawerSheet {
                        Tab.all.forEach { tab ->
                            NavigationDrawerItem(
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label) },
                                selected = selectedTab == tab,
                                onClick = {
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
                    }
                },
            ) {
                MainNavHost(navController = navController, modifier = Modifier.fillMaxSize())
            }
        } else {
            MainNavHost(navController = navController, modifier = Modifier.fillMaxSize())
        }
    } else {
        // Phone: bottom navigation bar
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                if (showBottomBar) {
                    val activeTab: Tab? = when {
                        currentDest?.route?.startsWith("movie_detail") == true -> Tab.Movies
                        currentDest?.route?.startsWith("show_detail") == true -> {
                            val contentType = navBackStackEntry?.arguments?.getString("contentType") ?: "show"
                            if (contentType == "anime") Tab.Anime else Tab.Series
                        }
                        else -> Tab.all.firstOrNull { t ->
                            currentDest?.hierarchy?.any { it.route == t.route } == true
                        }
                    }

                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        Tab.all.forEach { tab ->
                            NavigationBarItem(
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label) },
                                selected = activeTab == tab,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                ),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            MainNavHost(
                navController = navController,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun MainNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = Tab.Movies.route,
        modifier = modifier,
    ) {
        // ── Movies tab ────────────────────────────────────────────────────────
        composable(Tab.Movies.route) {
            MovieBrowserScreen(
                onMovieClick = { imdbId -> navController.navigate("movie_detail/${Uri.encode(imdbId)}") },
            )
        }
        composable(
            route = "movie_detail/{imdbId}",
            arguments = listOf(navArgument("imdbId") { type = NavType.StringType }),
        ) { backStack ->
            val imdbId = backStack.arguments!!.getString("imdbId")!!
            MovieDetailScreen(
                imdbId = imdbId,
                onBack = { navController.popBackStack() },
                onPlayClick = { quality -> navController.navigate("player/${Uri.encode(imdbId)}/${Uri.encode(quality)}") },
                onPlayDownload = { uri ->
                    navController.navigate("player_local/${Uri.encode(uri)}")
                },
            )
        }

        // ── Series tab ────────────────────────────────────────────────────────
        composable(Tab.Series.route) {
            ShowBrowserScreen(
                contentType = ContentType.SHOW,
                onShowClick = { imdbId -> navController.navigate("show_detail/${Uri.encode(imdbId)}/show") },
            )
        }
        composable(
            route = "show_detail/{imdbId}/{contentType}",
            arguments = listOf(
                navArgument("imdbId") { type = NavType.StringType },
                navArgument("contentType") { type = NavType.StringType },
            ),
        ) { backStack ->
            val imdbId = backStack.arguments!!.getString("imdbId")!!
            val showContentType = backStack.arguments!!.getString("contentType") ?: "show"
            ShowDetailScreen(
                imdbId = imdbId,
                onBack = { navController.popBackStack() },
                onEpisodePlay = { episodeImdbId, season, episode, quality ->
                    navController.navigate("player/${Uri.encode(episodeImdbId)}/${Uri.encode(quality)}?season=$season&episode=$episode&contentType=${Uri.encode(showContentType)}")
                },
                onPlayDownloaded = { uri ->
                    navController.navigate("player_local/${Uri.encode(uri)}")
                },
            )
        }

        // ── Anime tab ─────────────────────────────────────────────────────────
        composable(Tab.Anime.route) {
            ShowBrowserScreen(
                contentType = ContentType.ANIME,
                onShowClick = { imdbId -> navController.navigate("show_detail/${Uri.encode(imdbId)}/anime") },
            )
        }

        // ── Library tab ───────────────────────────────────────────────────────
        composable(Tab.Library.route) {
            LibraryScreen(
                onItemClick = { imdbId, contentType ->
                    when (contentType) {
                        LibraryContentType.MOVIE -> navController.navigate("movie_detail/${Uri.encode(imdbId)}")
                        LibraryContentType.SHOW -> navController.navigate("show_detail/${Uri.encode(imdbId)}/show")
                        LibraryContentType.ANIME -> navController.navigate("show_detail/${Uri.encode(imdbId)}/anime")
                    }
                },
                onSettings = { navController.navigate("settings") },
                onPlayDownload = { uri ->
                    navController.navigate("player_local/${Uri.encode(uri)}")
                },
            )
        }
        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onAppearanceSettings = { navController.navigate("settings/appearance") },
                onTorrentSettings = { navController.navigate("settings/torrent") },
                onSourceSettings = { navController.navigate("settings/sources") },
                onSubtitleSettings = { navController.navigate("settings/subtitles") },
                onRemoteSettings = { navController.navigate("settings/remote") },
                onTraktSettings = { navController.navigate("settings/trakt") },
            )
        }
        composable("settings/trakt") {
            TraktSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable("settings/subtitles") {
            SubtitleSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable("settings/sources") {
            SourceSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable("settings/remote") {
            RemoteSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable("settings/appearance") {
            AppearanceSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable("settings/torrent") {
            TorrentSettingsScreen(onBack = { navController.popBackStack() })
        }
        // ── Player (shared across all tabs) ───────────────────────────────────
        composable(
            route = "player/{imdbId}/{quality}?season={season}&episode={episode}&contentType={contentType}",
            arguments = listOf(
                navArgument("imdbId") { type = NavType.StringType },
                navArgument("quality") { type = NavType.StringType },
                navArgument("season") { type = NavType.IntType; defaultValue = -1 },
                navArgument("episode") { type = NavType.IntType; defaultValue = -1 },
                navArgument("contentType") { type = NavType.StringType; defaultValue = "movie" },
            ),
        ) { backStack ->
            val imdbId = backStack.arguments!!.getString("imdbId")!!
            val quality = backStack.arguments!!.getString("quality")!!
            val season = backStack.arguments!!.getInt("season")
            val episode = backStack.arguments!!.getInt("episode")
            val contentType = backStack.arguments!!.getString("contentType") ?: "movie"
            PlayerScreen(
                imdbId = imdbId,
                quality = quality,
                season = season,
                episode = episode,
                contentType = contentType,
                onBack = { navController.popBackStack() },
            )
        }

        // ── Local file player ─────────────────────────────────────────────────
        composable(
            route = "player_local/{localUri}",
            arguments = listOf(navArgument("localUri") { type = NavType.StringType }),
        ) { backStack ->
            val localUri = backStack.arguments!!.getString("localUri")!!
            PlayerScreen(
                imdbId = "",
                quality = "",
                localUri = localUri,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
