package com.thevault.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.thevault.ui.theme.*
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Collection : Screen("collection")
    object Wishlist : Screen("wishlist")
    object Consoles : Screen("consoles")
    object Accessories : Screen("accessories")
    object Statistics : Screen("statistics")
    object GameDetail : Screen("game_detail/{gameId}") {
        fun createRoute(gameId: String) = "game_detail/$gameId"
    }
    object ConsoleDetail : Screen("console_detail/{consoleId}") {
        fun createRoute(consoleId: String) = "console_detail/$consoleId"
    }
    object AccessoryDetail : Screen("accessory_detail/{accessoryId}") {
        fun createRoute(accessoryId: String) = "accessory_detail/$accessoryId"
    }
    object AddItem : Screen("add_item?gameId={gameId}") {
        fun createRoute(gameId: String? = null) =
            if (gameId != null) "add_item?gameId=$gameId" else "add_item"
    }
}

private data class VaultNavigationItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
)

@Composable
fun VaultNavHost(
    navController: NavHostController = rememberNavController(),
) {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val navigationItems = listOf(
        VaultNavigationItem(Screen.Dashboard, "Dashboard", Icons.Default.Dashboard),
        VaultNavigationItem(Screen.Collection, "Collection", Icons.Default.LibraryBooks),
        VaultNavigationItem(Screen.Wishlist, "Wishlist", Icons.Default.Star),
        VaultNavigationItem(Screen.Consoles, "Consoles", Icons.Default.Dns),
        VaultNavigationItem(Screen.Accessories, "Accessoires", Icons.Default.Extension),
        VaultNavigationItem(Screen.Statistics, "Statistiques", Icons.Default.BarChart),
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Surface,
                drawerContentColor = OnSurface,
                modifier = Modifier.width(300.dp)
            ) {
                Spacer(Modifier.height(24.dp))
                Text(
                    "THE VAULT",
                    modifier = Modifier.padding(start = 28.dp, bottom = 24.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = Primary,
                    letterSpacing = 2.sp
                )
                navigationItems.forEach { item ->
                    NavigationDrawerItem(
                        label = { Text(item.label) },
                        selected = currentRoute == item.screen.route,
                        onClick = {
                            if (currentRoute != item.screen.route) {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = Primary.copy(alpha = 0.1f),
                            selectedIconColor = Primary,
                            selectedTextColor = Primary,
                            unselectedIconColor = OnSurfaceVariant,
                            unselectedTextColor = OnSurfaceVariant
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }
    ) {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = SurfaceBright.copy(alpha = 0.6f),
                    modifier = Modifier.clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                ) {
                    navigationItems.take(4).forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.screen.route,
                            onClick = {
                                if (currentRoute != item.screen.route) {
                                    navController.navigate(item.screen.route) {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(imageVector = item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Primary,
                                unselectedIconColor = OnSurfaceVariant,
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                    NavigationBarItem(
                        selected = false,
                        onClick = { scope.launch { drawerState.open() } },
                        icon = { Icon(Icons.Default.Menu, contentDescription = "Menu") },
                        label = { Text("Menu") },
                        colors = NavigationBarItemDefaults.colors(
                            unselectedIconColor = OnSurfaceVariant,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = Screen.Dashboard.route,
                modifier = Modifier.padding(paddingValues)
            ) {
                composable(Screen.Dashboard.route) {
                    com.thevault.ui.screen.dashboard.DashboardScreen(
                        onAddItem = { navController.navigate(Screen.AddItem.route) },
                        onNavigateGames = {
                            navController.navigate(Screen.Collection.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onNavigateConsoles = {
                            navController.navigate(Screen.Consoles.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onNavigateAccessories = {
                            navController.navigate(Screen.Accessories.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onNavigateWishlist = {
                            navController.navigate(Screen.Wishlist.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onNavigateStatistics = {
                            navController.navigate(Screen.Statistics.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onMenuClick = { scope.launch { drawerState.open() } }
                    )
                }
                composable(Screen.Collection.route) {
                    com.thevault.ui.screen.collection.CollectionScreen(
                        onGameClick = { gameId ->
                            navController.navigate(Screen.GameDetail.createRoute(gameId))
                        }
                    )
                }
                composable(Screen.GameDetail.route) { backStackEntry ->
                    val gameId = backStackEntry.arguments?.getString("gameId") ?: return@composable
                    com.thevault.ui.screen.gamedetail.GameDetailScreen(
                        gameId = gameId,
                        onEdit = { navController.navigate(Screen.AddItem.createRoute(gameId)) },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Screen.Consoles.route) {
                    com.thevault.ui.screen.console.ConsoleScreen(
                        onConsoleClick = { consoleId ->
                            navController.navigate(Screen.ConsoleDetail.createRoute(consoleId))
                        }
                    )
                }
                composable(Screen.ConsoleDetail.route) { backStackEntry ->
                    val consoleId = backStackEntry.arguments?.getString("consoleId") ?: return@composable
                    com.thevault.ui.screen.console.ConsoleDetailScreen(
                        consoleId = consoleId,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Screen.Accessories.route) {
                    com.thevault.ui.screen.accessory.AccessoryScreen(
                        onAccessoryClick = { accessoryId ->
                            navController.navigate(Screen.AccessoryDetail.createRoute(accessoryId))
                        }
                    )
                }
                composable(Screen.AccessoryDetail.route) { backStackEntry ->
                    val accessoryId = backStackEntry.arguments?.getString("accessoryId") ?: return@composable
                    com.thevault.ui.screen.accessory.AccessoryDetailScreen(
                        accessoryId = accessoryId,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(
                    route = Screen.AddItem.route,
                    arguments = listOf(androidx.navigation.navArgument("gameId") {
                        type = androidx.navigation.NavType.StringType
                        nullable = true
                    })
                ) { backStackEntry ->
                    val gameId = backStackEntry.arguments?.getString("gameId")
                    com.thevault.ui.screen.additem.AddItemScreen(
                        gameId = gameId,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Screen.Wishlist.route) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Vue Wishlist - À implémenter")
                    }
                }
                composable(Screen.Statistics.route) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Vue Statistiques - À implémenter")
                    }
                }
            }
        }
    }
}
