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
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.thevault.ui.screen.accessory.AccessoryDetailScreen
import com.thevault.ui.screen.additem.AddItemScreen
import com.thevault.ui.screen.collection.CollectionEntry
import com.thevault.ui.screen.collection.CollectionScreen
import com.thevault.ui.screen.collection.CollectionViewModel
import com.thevault.ui.screen.collection.ItemKind
import com.thevault.ui.screen.collection.WishlistScreen
import com.thevault.ui.screen.console.ConsoleDetailScreen
import com.thevault.ui.screen.dashboard.DashboardActions
import com.thevault.ui.screen.dashboard.DashboardScreen
import com.thevault.ui.screen.gamedetail.GameDetailScreen
import com.thevault.ui.screen.settings.SettingsScreen
import com.thevault.ui.theme.Background
import org.koin.compose.viewmodel.koinViewModel

/**
 * Root navigation: bottom bar with four tabs + central "Add" button, and detail screens.
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
                    onAdd = { navController.navigate(Routes.addItem()) },
                )
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().background(Background).padding(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding())) {
            VaultGraph(navController)
        }
    }
}

@Composable
private fun VaultGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.DASHBOARD) {
        tabDestinations(navController)
        detailDestinations(navController)
    }
}

/** Root tabs: dashboard, collection, wishlist, settings. */
private fun NavGraphBuilder.tabDestinations(navController: NavHostController) {
    val openEntry: (CollectionEntry) -> Unit = { navController.navigate(Routes.detail(it)) }
    val openAdd: () -> Unit = { navController.navigate(Routes.addItem()) }
    composable(Routes.DASHBOARD) {
        DashboardScreen(
            actions =
                DashboardActions(
                    onOpenCollection = { navController.navigateToTab(Routes.collection(it)) },
                    onOpenWishlist = { navController.navigateToTab(Routes.WISHLIST) },
                    onEntryClick = openEntry,
                ),
        )
    }
    composable(Routes.COLLECTION, arguments = listOf(optionalStringArgument("kind"))) { entry ->
        val viewModel: CollectionViewModel = koinViewModel()
        val kind = entry.arguments?.getString("kind")?.let { name -> ItemKind.entries.find { it.name == name } }
        LaunchedEffect(kind) { if (kind != null) viewModel.onKindSelected(kind) }
        CollectionScreen(onEntryClick = openEntry, onAddItem = openAdd, viewModel = viewModel)
    }
    composable(Routes.WISHLIST) { WishlistScreen(onEntryClick = openEntry, onAddItem = openAdd) }
    composable(Routes.SETTINGS) { SettingsScreen() }
}

/** Detail and form screens, displayed without the bottom bar. */
private fun NavGraphBuilder.detailDestinations(navController: NavHostController) {
    val back: () -> Unit = { navController.popBackStack() }
    composable(Routes.GAME_DETAIL) { entry ->
        val id = entry.arguments?.getString("id") ?: return@composable
        GameDetailScreen(gameId = id, onEdit = { navController.navigate(Routes.addItem(id)) }, onBack = back)
    }
    composable(Routes.CONSOLE_DETAIL) { entry ->
        val id = entry.arguments?.getString("id") ?: return@composable
        ConsoleDetailScreen(consoleId = id, onBack = back)
    }
    composable(Routes.ACCESSORY_DETAIL) { entry ->
        val id = entry.arguments?.getString("id") ?: return@composable
        AccessoryDetailScreen(accessoryId = id, onBack = back)
    }
    composable(Routes.ADD_ITEM, arguments = listOf(optionalStringArgument("gameId"))) { entry ->
        AddItemScreen(gameId = entry.arguments?.getString("gameId"), onBack = back)
    }
}

private fun optionalStringArgument(name: String) =
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
