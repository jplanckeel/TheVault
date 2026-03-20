package com.thevault.ui.screen.collection

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.thevault.domain.model.Accessory
import com.thevault.domain.model.Game
import com.thevault.ui.component.PlatformIcon
import com.thevault.ui.theme.OnSecondaryContainer
import com.thevault.ui.theme.OnSurface
import com.thevault.ui.theme.OnSurfaceVariant
import com.thevault.ui.theme.Primary
import com.thevault.ui.theme.SecondaryContainer
import com.thevault.ui.theme.Surface
import com.thevault.ui.theme.SurfaceContainer
import com.thevault.ui.theme.SurfaceContainerHigh
import com.thevault.ui.theme.SurfaceContainerLow
import com.thevault.ui.theme.SurfaceVariant
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CollectionScreen(
    viewModel: CollectionViewModel = koinViewModel(),
    onGameClick: (String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    CollectionContent(
        uiState = uiState,
        onGameClick = onGameClick,
        onSearchQueryChange = viewModel::onSearchQueryChanged,
        onPlatformSelected = viewModel::onPlatformSelected,
    )
}

@Composable
fun CollectionContent(
    uiState: CollectionUiState,
    onGameClick: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onPlatformSelected: (String?) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
            .padding(top = 24.dp),
    ) {
        if (uiState.selectedPlatform != null) {
            GameListHeader(
                platformName = uiState.selectedPlatform,
                onBack = { onPlatformSelected(null) },
            )
        } else {
            CollectionOverviewHeader(
                platformCount = uiState.manufacturerGroups.sumOf { it.platforms.size },
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
            return@Column
        }

        if (uiState.selectedPlatform == null) {
            ManufacturerBannerList(
                groups = uiState.manufacturerGroups,
                onPlatformSelected = onPlatformSelected,
            )
        } else {
            GameListView(
                uiState = uiState,
                onGameClick = onGameClick,
                onSearchQueryChange = onSearchQueryChange,
            )
        }
    }
}

// ─── Headers ─────────────────────────────────────────────────────────────────

@Composable
private fun CollectionOverviewHeader(platformCount: Int) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Text(
            text = "MA COLLECTION",
            style = MaterialTheme.typography.labelSmall,
            color = OnSurfaceVariant,
            letterSpacing = 2.sp,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "$platformCount PLATEFORMES",
            style = MaterialTheme.typography.headlineMedium,
            color = OnSurface,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun GameListHeader(platformName: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Retour",
                tint = OnSurface,
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Column {
            Text(
                text = "MA COLLECTION",
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceVariant,
                letterSpacing = 2.sp,
            )
            Text(
                text = platformName,
                style = MaterialTheme.typography.displaySmall,
                color = OnSurface,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

// ─── Manufacturer Banner List ─────────────────────────────────────────────────

/**
 * Vertically scrollable list of manufacturer sections.
 * Each section shows a manufacturer header followed by stacked [PlatformBannerRow]s.
 */
@Composable
fun ManufacturerBannerList(
    groups: List<ManufacturerGroup>,
    onPlatformSelected: (String) -> Unit,
) {
    if (groups.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Aucun jeu dans la collection.", color = OnSurfaceVariant)
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        groups.forEachIndexed { groupIndex, group ->
            // Manufacturer header
            item(key = "header_${group.manufacturer}") {
                if (groupIndex > 0) Spacer(modifier = Modifier.height(8.dp))
                ManufacturerHeader(group = group)
                Spacer(modifier = Modifier.height(8.dp))
            }

            // One banner row per platform, stacked vertically
            items(
                items = group.platforms,
                key = { "platform_${it.name}" },
            ) { entry ->
                PlatformBannerRow(
                    entry = entry,
                    onClick = { onPlatformSelected(entry.name) },
                )
            }

            // Spacer between manufacturer groups
            item(key = "spacer_${group.manufacturer}") {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * Manufacturer section header with name and platform count.
 */
@Composable
private fun ManufacturerHeader(group: ManufacturerGroup) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = group.manufacturer.uppercase(),
            style = MaterialTheme.typography.titleMedium,
            color = Primary,
            letterSpacing = 2.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "${group.platforms.size} plateforme${if (group.platforms.size > 1) "s" else ""}",
            style = MaterialTheme.typography.labelSmall,
            color = OnSurfaceVariant,
            fontSize = 10.sp,
        )
    }
}

/**
 * Full-width banner row for a single platform.
 * Shows the console thumbnail on the left, platform name + year + game count on the right,
 * with a cover image as a subtle background.
 */
@Composable
fun PlatformBannerRow(
    entry: PlatformEntry,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 5.dp)
            .height(80.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerLow)
            .clickable { onClick() },
    ) {
        // Decorative cover background (right side, faded)
        if (entry.coverUrls.isNotEmpty()) {
            AsyncImage(
                model = entry.coverUrls.first(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.12f,
            )
        }

        // Left-to-right gradient scrim for readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to SurfaceContainerLow.copy(alpha = 1f),
                        0.55f to SurfaceContainerLow.copy(alpha = 0.85f),
                        1f to SurfaceContainerLow.copy(alpha = 0.2f),
                    ),
                ),
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Console thumbnail
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceContainer),
                contentAlignment = Alignment.Center,
            ) {
                if (entry.console?.imageUrl != null) {
                    AsyncImage(
                        model = entry.console.imageUrl,
                        contentDescription = entry.name,
                        modifier = Modifier
                            .size(40.dp)
                            .padding(4.dp),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    PlatformIcon(
                        platformName = entry.name,
                        manufacturer = entry.manufacturer,
                        size = 28.dp,
                        tint = OnSurfaceVariant,
                    )
                }
            }

            // Platform info
            Column(modifier = Modifier.weight(1f)) {
                if (entry.productionYear > 0) {
                    Text(
                        text = entry.productionYear.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp,
                    )
                }
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = OnSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }

            // Game count badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceContainerHigh)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "${entry.gameCount} jeux",
                    style = MaterialTheme.typography.labelSmall,
                    color = Primary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

// ─── Game List View ───────────────────────────────────────────────────────────

@Composable
private fun GameListView(
    uiState: CollectionUiState,
    onGameClick: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = uiState.searchQuery,
        onValueChange = onSearchQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        placeholder = { Text("Rechercher un jeu...", color = OnSurfaceVariant) },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null, tint = OnSurfaceVariant)
        },
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Primary,
            unfocusedBorderColor = SurfaceVariant,
            focusedContainerColor = SurfaceContainerLow,
            unfocusedContainerColor = SurfaceContainerLow,
            focusedTextColor = OnSurface,
            unfocusedTextColor = OnSurface,
        ),
        singleLine = true,
    )
    Spacer(modifier = Modifier.height(16.dp))

    LazyColumn(
        contentPadding = PaddingValues(bottom = 24.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (uiState.accessories.isNotEmpty()) {
            item {
                AccessoriesSection(accessories = uiState.accessories)
            }
        }

        item {
            Text(
                text = "${uiState.games.size} JEUX",
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp),
                letterSpacing = 2.sp,
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            GamesGrid(games = uiState.games, onGameClick = onGameClick)
        }
    }
}

// ─── Accessories Section ──────────────────────────────────────────────────────

/**
 * Horizontal scrollable row of accessories compatible with the selected platform.
 */
@Composable
fun AccessoriesSection(accessories: List<Accessory>) {
    Column {
        Text(
            text = "ACCESSOIRES",
            style = MaterialTheme.typography.labelSmall,
            color = OnSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp),
            letterSpacing = 2.sp,
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(accessories) { accessory ->
                AccessoryChip(accessory = accessory)
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

/**
 * Compact accessory chip shown in [AccessoriesSection].
 */
@Composable
fun AccessoryChip(accessory: Accessory) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainerLow)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceContainer),
            contentAlignment = Alignment.Center,
        ) {
            if (accessory.imageUrl != null) {
                AsyncImage(
                    model = accessory.imageUrl,
                    contentDescription = accessory.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            } else {
                Text(text = "🔧", fontSize = 18.sp)
            }
        }
        Column {
            Text(
                text = accessory.type.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceVariant,
                fontSize = 9.sp,
                letterSpacing = 1.sp,
            )
            Text(
                text = accessory.name,
                style = MaterialTheme.typography.labelMedium,
                color = OnSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}

// ─── Games Grid ───────────────────────────────────────────────────────────────

/**
 * Manual 2-column grid of [GameCard]s, safe to embed inside a [LazyColumn].
 */
@Composable
fun GamesGrid(games: List<Game>, onGameClick: (String) -> Unit) {
    if (games.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Aucun jeu trouvé.", color = OnSurfaceVariant)
        }
        return
    }

    val rows = games.chunked(2)
    Column(
        modifier = Modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        rows.forEach { rowGames ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                rowGames.forEach { game ->
                    GameCard(
                        game = game,
                        onClick = { onGameClick(game.id) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowGames.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

// ─── Game Card ────────────────────────────────────────────────────────────────

/**
 * Card displaying a game's cover, platform label, title and status chip.
 */
@Composable
fun GameCard(
    game: Game,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainerLow)
            .clickable { onClick() }
            .padding(bottom = 12.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceVariant),
        ) {
            if (game.coverUrl != null) {
                AsyncImage(
                    model = game.coverUrl,
                    contentDescription = game.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(SecondaryContainer)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(
                    text = game.status.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSecondaryContainer,
                    fontSize = 10.sp,
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Column(modifier = Modifier.padding(horizontal = 12.dp)) {
            Text(
                text = game.platform.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceVariant,
                letterSpacing = 1.sp,
                fontSize = 10.sp,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = game.title,
                style = MaterialTheme.typography.titleMedium,
                color = OnSurface,
                maxLines = 2,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
