package com.thevault.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.savedstate.read
import com.thevault.ui.screen.accessory.AccessoryDetailScreen
import com.thevault.ui.screen.add.AddScreen
import com.thevault.ui.screen.additem.AddItemScreen
import com.thevault.ui.screen.collection.CollectionEntry
import com.thevault.ui.screen.collection.CollectionNavigation
import com.thevault.ui.screen.collection.CollectionScreen
import com.thevault.ui.screen.collection.CollectionViewModel
import com.thevault.ui.screen.collection.ItemKind
import com.thevault.ui.screen.collection.PlatformGamesScreen
import com.thevault.ui.screen.console.ConsoleDetailScreen
import com.thevault.ui.screen.gamedetail.GameDetailScreen
import com.thevault.ui.screen.home.HomeActions
import com.thevault.ui.screen.home.HomeScreen
import com.thevault.ui.screen.settings.SettingsScreen
import com.thevault.ui.screen.stats.StatsScreen
import com.thevault.ui.screen.wishlist.WishlistScreen
import com.thevault.ui.theme.Background
import org.koin.compose.viewmodel.koinViewModel

/**
 * Root navigation: opaque bottom bar (Accueil, Collection, +, Wishlist, Stats) and the
 * detail / add screens displayed without it.
 *
 * @param navController Navigation controller.
 */
@Composable
fun VaultNavHost(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isRootTab = VaultTab.entries.any { it.route == currentRoute }

    Scaffold(
        containerColor = Background,
        bottomBar = {
            if (isRootTab) {
                VaultBottomBar(
                    currentRoute = currentRoute,
                    onTabSelected = { navController.navigateToTab(it.route.substringBefore('?')) },
                    onAdd = { navController.navigate(Routes.ADD) },
                )
            }
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .background(Background)
                .padding(padding),
        ) {
            NavHost(navController = navController, startDestination = Routes.HOME) {
                tabDestinations(navController)
                detailDestinations(navController)
            }
        }
    }
}

/** Root tabs. */
private fun NavGraphBuilder.tabDestinations(nav: NavHostController) {
    val openEntry: (CollectionEntry) -> Unit = { nav.navigate(Routes.detail(it)) }
    val openAdd: () -> Unit = { nav.navigate(Routes.ADD) }
    composable(Routes.HOME) {
        HomeScreen(
            HomeActions(
                onSearch = { nav.navigateToTab(Routes.collection(ItemKind.Game)) },
                onSettings = { nav.navigate(Routes.SETTINGS) },
                onOpenCollection = { nav.navigateToTab(Routes.collection(it)) },
                onOpenPlatform = { nav.navigate(Routes.platform(it)) },
                onOpenStats = { nav.navigateToTab(Routes.STATS) },
                onEntryClick = openEntry,
                onAdd = openAdd,
            ),
        )
    }
    composable(Routes.COLLECTION, arguments = listOf(optionalString("kind"))) { entry ->
        val viewModel: CollectionViewModel = koinViewModel()
        val kind = entry.stringArg("kind")?.let { name -> ItemKind.entries.find { it.name == name } }
        LaunchedEffect(kind) { if (kind != null) viewModel.onKindSelected(kind) }
        CollectionScreen(CollectionNavigation(openEntry, { nav.navigate(Routes.platform(it)) }, openAdd), viewModel)
    }
    composable(Routes.WISHLIST) { WishlistScreen(onEntryClick = openEntry, onAdd = openAdd) }
    composable(Routes.STATS) { StatsScreen() }
}

/** Screens displayed without the bottom bar. */
private fun NavGraphBuilder.detailDestinations(nav: NavHostController) {
    val back: () -> Unit = { nav.popBackStack() }
    composable(Routes.PLATFORM, arguments = listOf(optionalString("name"))) { entry ->
        PlatformGamesScreen(entry.stringArg("name"), back, { nav.navigate(Routes.detail(it)) })
    }
    composable(Routes.SETTINGS) { SettingsScreen(onBack = back) }
    composable(Routes.GAME_DETAIL) { entry ->
        val id = entry.stringArg("id") ?: return@composable
        GameDetailScreen(gameId = id, onBack = back, onEdit = { nav.navigate(Routes.editGame(id)) })
    }
    composable(Routes.CONSOLE_DETAIL) { entry ->
        val id = entry.stringArg("id") ?: return@composable
        ConsoleDetailScreen(consoleId = id, onBack = back)
    }
    composable(Routes.ACCESSORY_DETAIL) { entry ->
        val id = entry.stringArg("id") ?: return@composable
        AccessoryDetailScreen(accessoryId = id, onBack = back)
    }
    composable(Routes.ADD) {
        AddScreen(onClose = back, onAddDetails = { gameId ->
            nav.navigate(Routes.editGame(gameId)) { popUpTo(Routes.ADD) { inclusive = true } }
        })
    }
    composable(Routes.EDIT_GAME, arguments = listOf(optionalString("gameId"))) { entry ->
        AddItemScreen(gameId = entry.stringArg("gameId"), onBack = back)
    }
}

/** Reads a string navigation argument (multiplatform: Bundle on Android, SavedState on iOS). */
private fun NavBackStackEntry.stringArg(name: String): String? = arguments?.read { getStringOrNull(name) }

private fun optionalString(name: String) =
    navArgument(name) {
        type = NavType.StringType
        nullable = true
    }

/** Navigates to a root tab, keeping a single copy of each tab on the back stack. */
private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
