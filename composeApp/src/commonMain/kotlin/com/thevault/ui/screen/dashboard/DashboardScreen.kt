package com.thevault.ui.screen.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thevault.domain.model.VaultStats
import com.thevault.ui.theme.Primary
import com.thevault.ui.theme.PrimaryDim
import com.thevault.ui.theme.Surface
import com.thevault.ui.theme.SurfaceContainer
import com.thevault.ui.theme.SurfaceContainerLow
import com.thevault.ui.theme.Tertiary
import com.thevault.ui.theme.OnSurface
import com.thevault.ui.theme.OnSurfaceVariant
import com.thevault.domain.model.Game
import com.thevault.ui.theme.Background
import coil3.compose.AsyncImage
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.IconButton
import org.koin.compose.viewmodel.koinViewModel

@Composable
private fun DashboardHeader(onMenuClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "TABLEAU DE BORD",
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceVariant,
                letterSpacing = 2.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Ma Galerie Digitale",
                style = MaterialTheme.typography.displayMedium,
                color = OnSurface,
                fontWeight = FontWeight.Bold,
            )
        }
        
        IconButton(
            onClick = onMenuClick,
            modifier = Modifier
                .clip(CircleShape)
                .background(SurfaceContainer)
        ) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Menu",
                tint = OnSurface
            )
        }
    }
}

@Composable
fun DashboardScreen(
    onAddItem: () -> Unit,
    onNavigateGames: () -> Unit = {},
    onNavigateConsoles: () -> Unit = {},
    onNavigateAccessories: () -> Unit = {},
    onNavigateWishlist: () -> Unit = {},
    onNavigateStatistics: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    viewModel: DashboardViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState(DashboardUiState(isLoading = true))

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddItem,
                containerColor = Primary,
                contentColor = Color.Black,
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Ajouter")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            DashboardContent(
                uiState = uiState,
                onNavigateGames = onNavigateGames,
                onNavigateConsoles = onNavigateConsoles,
                onNavigateAccessories = onNavigateAccessories,
                onNavigateWishlist = onNavigateWishlist,
                onMenuClick = onMenuClick,
                onImportCsv = viewModel::importCsv,
                onExportCsv = viewModel::exportCsv,
                onSyncIgdb = viewModel::syncIgdb,
                clearMessage = viewModel::clearMessage
            )
        }
    }
}

@Composable
fun DashboardContent(
    uiState: DashboardUiState,
    onNavigateGames: () -> Unit,
    onNavigateConsoles: () -> Unit,
    onNavigateAccessories: () -> Unit,
    onNavigateWishlist: () -> Unit,
    onMenuClick: () -> Unit,
    onImportCsv: (String) -> Unit,
    onExportCsv: ((String) -> Unit) -> Unit,
    onSyncIgdb: () -> Unit,
    clearMessage: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface),
        contentPadding = PaddingValues(top = 24.dp, bottom = 120.dp, start = 24.dp, end = 24.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        item { DashboardHeader(onMenuClick = onMenuClick) }

        if (!uiState.isLoading) {
            item { FinancialOverview(uiState.stats) }
        }

        item {
            CategoryGrid(
                stats = uiState.stats,
                onNavigateGames = onNavigateGames,
                onNavigateConsoles = onNavigateConsoles,
                onNavigateAccessories = onNavigateAccessories,
                onNavigateWishlist = onNavigateWishlist,
            )
        }

        if (uiState.latestGames.isNotEmpty()) {
            item {
                LatestAdditionsSection(
                    games = uiState.latestGames,
                    onViewAll = onNavigateGames
                )
            }
        }

        if (uiState.topRatedGames.isNotEmpty()) {
            item {
                HallOfFameSection(
                    games = uiState.topRatedGames
                )
            }
        }

        item {
            val exportContent = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
            
            Column {
                CsvImportExportSection(
                    onImport = onImportCsv,
                    onExport = {
                        onExportCsv { content ->
                            exportContent.value = content
                        }
                    },
                    exportContentProvider = { exportContent.value }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onSyncIgdb,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SurfaceContainerLow,
                        contentColor = Primary
                    )
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Sync", modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Synchroniser avec IGDB", fontWeight = FontWeight.Bold)
                }
                
                if (uiState.message != null) {
                    androidx.compose.runtime.LaunchedEffect(uiState.message) {
                        kotlinx.coroutines.delay(5000)
                        clearMessage()
                    }
                    Text(
                        text = uiState.message.toString(),
                        color = Primary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LatestAdditionsSection(
    games: List<Game>,
    onViewAll: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "DERNIÈRES ADDITIONS",
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceVariant,
                letterSpacing = 2.sp,
            )
            Text(
                text = "Voir tout",
                style = MaterialTheme.typography.labelMedium,
                color = Primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onViewAll() }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            games.forEach { game ->
                LatestGameCard(
                    game = game,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun LatestGameCard(
    game: Game,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerLow)
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceContainer)
        ) {
            if (game.coverUrl != null) {
                AsyncImage(
                    model = game.coverUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = game.title,
            style = MaterialTheme.typography.labelLarge,
            color = OnSurface,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
        Text(
            text = game.platform,
            style = MaterialTheme.typography.labelSmall,
            color = OnSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun HallOfFameSection(
    games: List<Game>
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "HALL OF FAME",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant,
                    letterSpacing = 2.sp,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Verified,
                        contentDescription = null,
                        tint = Tertiary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "powered by Metacritic",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = OnSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
            }
        }

        games.forEach { game ->
            HallOfFameItem(game)
        }
    }
}

@Composable
private fun HallOfFameItem(game: Game) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(listOf(SurfaceContainerLow, SurfaceContainer)))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceContainer)
        ) {
            if (game.coverUrl != null) {
                AsyncImage(
                    model = game.coverUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }
        }
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = game.title,
                style = MaterialTheme.typography.titleMedium,
                color = OnSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                text = game.platform,
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant
            )
        }

        Box(
            modifier = Modifier
                .size(44.dp)
                .border(2.dp, Tertiary.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = game.rating?.toString() ?: "—",
                style = MaterialTheme.typography.titleMedium,
                color = Tertiary,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun CategoryGrid(
    stats: VaultStats,
    onNavigateGames: () -> Unit,
    onNavigateConsoles: () -> Unit,
    onNavigateAccessories: () -> Unit,
    onNavigateWishlist: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "CATÉGORIES",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 2.sp,
        )
        // Top row: Jeux (large) + Consoles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CategoryCard(
                emoji = "🎮",
                label = "Jeux",
                count = stats.totalGames,
                accentColor = Primary,
                onClick = onNavigateGames,
                modifier = Modifier.weight(1.4f),
            )
            CategoryCard(
                emoji = "🕹️",
                label = "Consoles",
                count = stats.totalConsoles,
                accentColor = Tertiary,
                onClick = onNavigateConsoles,
                modifier = Modifier.weight(1f),
            )
        }
        // Bottom row: Accessoires + Wishlist
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CategoryCard(
                emoji = "🔧",
                label = "Accessoires",
                count = stats.totalAccessories,
                accentColor = Color(0xFF63EFEC),
                onClick = onNavigateAccessories,
                modifier = Modifier.weight(1f),
            )
            CategoryCard(
                emoji = "⭐",
                label = "Wishlist",
                count = stats.totalWishlist,
                accentColor = Color(0xFFFFD166),
                onClick = onNavigateWishlist,
                modifier = Modifier.weight(1.4f),
            )
        }
    }
}

@Composable
private fun CategoryCard(
    emoji: String,
    label: String,
    count: Int,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainer)
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(20.dp),
    ) {
        Column {
            Text(
                text = emoji,
                fontSize = 32.sp,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.displaySmall,
                color = accentColor,
                fontWeight = FontWeight.Black,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FinancialOverview(stats: VaultStats) {
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Aesthetic Aura Blobs behind the card (simulating pseudo-elements)
        Box(
            modifier = Modifier
                .size(150.dp)
                .align(Alignment.TopStart)
                .offset(x = (-30).dp, y = (-30).dp)
                .background(Primary.copy(alpha = 0.15f), CircleShape)
                .blur(80.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
        )
        Box(
            modifier = Modifier
                .size(150.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 30.dp, y = 30.dp)
                .background(Tertiary.copy(alpha = 0.15f), CircleShape)
                .blur(80.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
        )

        // Main Stat Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceContainerLow)
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                .padding(32.dp),
        ) {
            Text(
                text = "VALEUR TOTALE DU VAULT",
                style = MaterialTheme.typography.labelSmall,
                color = Primary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "${stats.estimatedValue} €",
                    fontSize = 48.sp,
                    color = OnSurface,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
                )
                
                val ratio = if (stats.totalInvested > 0) {
                    (stats.estimatedValue - stats.totalInvested) / stats.totalInvested * 100
                } else 0.0
                
                if (ratio != 0.0) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "${if (ratio > 0) "+" else ""}${ratio.toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (ratio > 0) Tertiary else Color(0xFFF87171),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ÉLÉMENTS",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${stats.totalGames + stats.totalConsoles + stats.totalAccessories}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = OnSurface,
                        fontWeight = FontWeight.Bold,
                    )
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "INVESTI",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${stats.totalInvested} €",
                        style = MaterialTheme.typography.headlineSmall,
                        color = OnSurface,
                        fontWeight = FontWeight.Bold,
                    )
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    val sign = if (stats.estimatedReturn >= 0) "+" else ""
                    Text(
                        text = "PLUS-VALUE",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$sign${stats.estimatedReturn} €",
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (stats.estimatedReturn >= 0) Color(0xFF4ADE80) else Color(0xFFF87171),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
