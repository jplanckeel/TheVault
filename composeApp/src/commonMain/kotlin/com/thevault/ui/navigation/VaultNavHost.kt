package com.thevault.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
import com.thevault.ui.app.AppViewModel
import com.thevault.ui.screen.add.AddScreen
import com.thevault.ui.screen.additem.AddItemScreen
import com.thevault.ui.screen.collection.CollectionEntry
import com.thevault.ui.screen.collection.CollectionNavigation
import com.thevault.ui.screen.collection.CollectionScreen
import com.thevault.ui.screen.collection.CollectionViewModel
import com.thevault.ui.screen.collection.ItemKind
import com.thevault.ui.screen.collection.PlatformGamesScreen
import com.thevault.ui.screen.gamedetail.GameDetailScreen
import com.thevault.ui.screen.home.HomeActions
import com.thevault.ui.screen.home.HomeScreen
import com.thevault.ui.screen.item.ItemDetailScreen
import com.thevault.ui.screen.item.ItemEditScreen
import com.thevault.ui.screen.profile.ProfileScreen
import com.thevault.ui.screen.report.QuoteReportScreen
import com.thevault.ui.screen.settings.SettingsScreen
import com.thevault.ui.screen.stats.StatsScreen
import com.thevault.ui.screen.wishlist.WishlistScreen
import org.koin.compose.viewmodel.koinViewModel

/**
 * Root navigation with the floating tab bar (Accueil, Collection, +, Envies, Profil),
 * hidden on the detail, add and settings screens.
 *
 * @param app Application ViewModel (shell, sync status).
 * @param navController Navigation controller.
 */
@Composable
fun VaultNavHost(
    app: AppViewModel,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val tab = VaultTab.of(backStackEntry?.destination?.route)
    Box(Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = Routes.HOME) {
            tabDestinations(navController, app)
            detailDestinations(navController, app)
        }
        if (tab != null) {
            VaultBottomBar(
                current = tab,
                onTabSelected = { navController.navigateToTab(it.route.substringBefore('?')) },
                onAdd = { navController.navigate(Routes.ADD) },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

/** Screens shown with the tab bar. */
private fun NavGraphBuilder.tabDestinations(
    nav: NavHostController,
    app: AppViewModel,
) {
    val openEntry: (CollectionEntry) -> Unit = { nav.navigate(Routes.detail(it)) }
    val openAdd: () -> Unit = { nav.navigate(Routes.ADD) }
    composable(Routes.HOME) {
        HomeScreen(
            HomeActions(
                onSearch = { nav.navigateToTab(Routes.collection(ItemKind.Game)) },
                onOpenCollection = { nav.navigateToTab(Routes.collection(it)) },
                onOpenRisers = { nav.navigate(Routes.platform(null)) },
                onEntryClick = openEntry,
                onAdd = openAdd,
                onSync = { nav.navigate(Routes.SETTINGS) },
                onOpenReport = { nav.navigate(Routes.REPORT) },
            ),
            app = app,
        )
    }
    composable(Routes.COLLECTION, arguments = listOf(optionalString("kind"))) { entry ->
        val viewModel: CollectionViewModel = koinViewModel()
        val kind = entry.stringArg("kind")?.let { name -> ItemKind.entries.find { it.name == name } }
        LaunchedEffect(kind) { if (kind != null) viewModel.onKindSelected(kind) }
        CollectionScreen(CollectionNavigation(openEntry, { nav.navigate(Routes.platform(it)) }, openAdd), viewModel)
    }
    composable(Routes.PLATFORM, arguments = listOf(optionalString("name"))) { entry ->
        PlatformGamesScreen(entry.stringArg("name"), { nav.popBackStack() }, openEntry)
    }
    composable(Routes.WISHLIST) { WishlistScreen(onEntryClick = openEntry, onAdd = openAdd) }
    composable(Routes.PROFILE) {
        ProfileScreen(onStats = { nav.navigate(Routes.STATS) }, onSettings = { nav.navigate(Routes.SETTINGS) })
    }
    composable(Routes.STATS) {
        StatsScreen(onBack = { nav.popBackStack() }, onEntryClick = openEntry, onExport = { nav.navigate(Routes.SETTINGS) })
    }
}

/** Screens displayed without the tab bar. */
private fun NavGraphBuilder.detailDestinations(
    nav: NavHostController,
    app: AppViewModel,
) {
    val back: () -> Unit = { nav.popBackStack() }
    composable(Routes.REPORT) { QuoteReportScreen(onBack = back, onEntryClick = { nav.navigate(Routes.detail(it)) }) }
    composable(Routes.SETTINGS) { SettingsScreen(app = app, onBack = back) }
    composable(Routes.GAME_DETAIL) { entry ->
        val id = entry.stringArg("id") ?: return@composable
        GameDetailScreen(gameId = id, onBack = back, onEdit = { nav.navigate(Routes.editGame(id)) })
    }
    composable(Routes.CONSOLE_DETAIL) { entry ->
        val id = entry.stringArg("id") ?: return@composable
        ItemDetailScreen(ItemKind.Console, id, back, onEdit = { nav.navigate(Routes.editItem(ItemKind.Console, id)) })
    }
    composable(Routes.ACCESSORY_DETAIL) { entry ->
        val id = entry.stringArg("id") ?: return@composable
        ItemDetailScreen(ItemKind.Accessory, id, back, onEdit = { nav.navigate(Routes.editItem(ItemKind.Accessory, id)) })
    }
    composable(Routes.ADD) {
        AddScreen(onClose = back, onAddDetails = { gameId ->
            nav.navigate(Routes.editGame(gameId)) { popUpTo(Routes.ADD) { inclusive = true } }
        })
    }
    composable(Routes.EDIT_ITEM) { entry ->
        val kind = entry.stringArg("kind")?.let { k -> ItemKind.entries.find { it.name == k } } ?: return@composable
        val id = entry.stringArg("id") ?: return@composable
        ItemEditScreen(kind, id, back)
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
