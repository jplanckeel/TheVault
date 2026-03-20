package com.thevault.ui.screen.gamedetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.thevault.domain.model.Game
import com.thevault.domain.model.ItemCondition
import com.thevault.ui.theme.Background
import com.thevault.ui.theme.CtaGradientColors
import com.thevault.ui.theme.ErrorContainer
import com.thevault.ui.theme.OnErrorContainer
import com.thevault.ui.theme.OnPrimaryFixed
import com.thevault.ui.theme.OnSecondaryContainer
import com.thevault.ui.theme.OnSurface
import com.thevault.ui.theme.OnSurfaceVariant
import com.thevault.ui.theme.OutlineVariant
import com.thevault.ui.theme.Primary
import com.thevault.ui.theme.PrimaryDim
import com.thevault.ui.theme.SecondaryContainer
import com.thevault.ui.theme.SurfaceBright
import com.thevault.ui.theme.SurfaceContainer
import com.thevault.ui.theme.SurfaceContainerHigh
import com.thevault.ui.theme.SurfaceContainerLow
import com.thevault.ui.theme.SurfaceContainerLowest
import com.thevault.ui.theme.Tertiary
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun GameDetailScreen(
    gameId: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    viewModel: GameDetailViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(gameId) {
        viewModel.loadGame(gameId)
    }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            onBack()
        }
    }

    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Supprimer l'item") },
            text = { Text("Êtes-vous sûr de vouloir supprimer définitivement cet item de votre collection ? Cette action est irréversible.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteGame()
                }) {
                    Text("Supprimer", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Annuler", color = Color.White)
                }
            },
            containerColor = SurfaceContainerLow,
            titleContentColor = Color.White,
            textContentColor = OnSurfaceVariant
        )
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(scrollState),
    ) {
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp).height(400.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        } else {
            uiState.game?.let { game ->
                HeroSection(
                    game = game,
                    onBack = onBack,
                    onEdit = onEdit,
                    onDelete = { showDeleteDialog = true }
                )
                
                ContentLayout(game = game)
            }
        }
    }
}

@Composable
fun HeroSection(
    game: Game,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 24.dp)
            .height(500.dp)
            .clip(RoundedCornerShape(24.dp))
    ) {
        // Background Image
        if (game.coverUrl != null) {
            AsyncImage(
                model = game.coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().background(SurfaceContainerHigh))
        }

        // Overlay Gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Background.copy(alpha = 0.4f), Background.copy(alpha = 1f)),
                    startY = 0f,
                    endY = 1500f // smooth fade out
                ))
        )

        // Top Navigation Bar (inside Hero)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopStart),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(SurfaceBright.copy(alpha = 0.5f))
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = OnSurface)
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(ErrorContainer.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = OnErrorContainer)
            }
        }

        // Bottom Left Elements
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp)
        ) {
            // Badges
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(SecondaryContainer)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = game.platform.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSecondaryContainer,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                if (game.isLimitedEdition || game.isCollectorEdition) {
                    val txt = if (game.isCollectorEdition) "ÉDITION COLLECTOR" else "ÉDITION LIMITÉE"
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(percent = 50))
                            .background(SurfaceBright.copy(alpha = 0.8f))
                            .border(1.dp, Primary.copy(alpha = 0.5f), RoundedCornerShape(percent = 50))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = txt,
                            style = MaterialTheme.typography.labelSmall,
                            color = Primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // Title
            Text(
                text = game.title,
                style = MaterialTheme.typography.displayMedium,
                color = OnSurface,
                fontWeight = FontWeight.Black,
                lineHeight = 44.sp,
                modifier = Modifier.fillMaxWidth(0.9f)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Edit Button
            Button(
                onClick = onEdit,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(CtaGradientColors))
                    .padding(horizontal = 24.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = OnPrimaryFixed, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(8.dp))
                Text("Éditer l'item", color = OnPrimaryFixed, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ContentLayout(game: Game) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Specifications
        SpecificationsCard(game)

        // Rating
        // OpenCritic
        EvaluationCard(game.rating, game.title)

        // Condition State
        ConditionCard(game.condition)

        // Financials Placeholder
        FinancialsCard(game)
        
        // Notes
        val notes = game.notes
        if (!notes.isNullOrBlank()) {
            NotesCard(notes)
        }

        // External Links
        ExternalLinksSection(
            query = "${game.title} ${game.platform}",
            youtubeQuery = "${game.title} ${game.platform} gameplay review"
        )
        
        Spacer(modifier = Modifier.height(64.dp))
    }
}

@Composable
fun SpecificationsCard(game: Game) {
    val items = listOfNotNull(
        if (game.genre.isNotBlank() && game.genre != "N/A") "Genre" to game.genre else null,
        "Plateforme" to game.platform,
        if (!game.developer.isNullOrBlank()) "Développeur" to game.developer else null,
        if (!game.publisher.isNullOrBlank()) "Éditeur" to game.publisher else null,
        "Statut" to game.status.name,
        "Année" to game.releaseYear.toString().takeIf { it != "0" }
    ).filterNotNull()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerLow)
            .padding(24.dp)
    ) {
        Text(
            text = "SPÉCIFICATIONS",
            style = MaterialTheme.typography.labelSmall,
            color = Primary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items.forEach { (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = label, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
                    Text(text = value ?: "—", style = MaterialTheme.typography.bodyMedium, color = OnSurface, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun EvaluationCard(rating: Int?, gameTitle: String) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerHigh)
            .clickable {
                // Simple encoding for URL:
                val query = gameTitle.replace(" ", "+")
                uriHandler.openUri("https://opencritic.com/search?text=$query")
            }
            .padding(24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "OPENCRITIC",
                style = MaterialTheme.typography.labelSmall,
                color = Tertiary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(text = if (rating != null && rating > 0) rating.toString() else "—", style = MaterialTheme.typography.displayMedium, color = OnSurface, fontWeight = FontWeight.Black)
                Text(text = "/100", style = MaterialTheme.typography.titleMedium, color = OnSurfaceVariant, fontWeight = FontWeight.Bold)
            }
        }
        
        Box(
            modifier = Modifier
                .size(64.dp)
                .border(4.dp, Tertiary, RoundedCornerShape(percent = 50)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Verified, contentDescription = null, tint = Tertiary, modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
fun ConditionCard(currentCondition: ItemCondition) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainer)
            .padding(24.dp)
    ) {
        Text(
            text = "RATING DE L'ÉTAT",
            style = MaterialTheme.typography.labelSmall,
            color = Primary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        val states = listOf(
            ItemCondition.LOOSE to ("Loose" to Icons.Default.Album),
            ItemCondition.CIB_NO_MANUAL to ("Boîte" to Icons.Default.MenuBook),
            ItemCondition.CIB to ("Complet" to Icons.Default.CheckCircle),
            ItemCondition.NEW_SEALED to ("Scellé" to Icons.Default.Inventory)
        )

        val gridConditions = listOf(ItemCondition.LOOSE, ItemCondition.CIB_NO_MANUAL, ItemCondition.CIB, ItemCondition.NEW_SEALED)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            gridConditions.forEach { cond ->
                val (label, icon) = states.firstOrNull { it.first == cond }?.second ?: ("" to Icons.Default.CheckCircle)
                val isSelected = currentCondition == cond
                
                // Active status fallback matching logic (e.g., Fair -> Loose)
                // Assuming exact match for these 4 for simplicity in display, though domain model might have more.
                // We will highlight the closest or the exact.
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceContainerLowest.copy(alpha = 0.5f))
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) PrimaryDim else OutlineVariant.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(icon, contentDescription = null, tint = if (isSelected) Primary else OnSurfaceVariant, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(label, color = if (isSelected) OnSurface else OnSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun FinancialsCard(game: Game) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerLow)
            .padding(24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text("HISTORIQUE DES PRIX", style = MaterialTheme.typography.labelSmall, color = Primary, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${game.currentValue ?: 0.0} €",
                        style = MaterialTheme.typography.headlineLarge,
                        color = OnSurface,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Valeur estimée",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Investi", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                Text("${game.purchasePrice ?: "—"} €", style = MaterialTheme.typography.titleMedium, color = OnSurface)
            }
            
            val current = game.currentValue
            val purchase = game.purchasePrice
            val roi = if (current != null && purchase != null) {
                current - purchase
            } else null
            
            if (roi != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("Plus-value / ROI", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                    Text("${if (roi >= 0) "+" else ""}$roi €", style = MaterialTheme.typography.titleMedium, color = if (roi >=0) Color(0xFF4ADE80) else Color(0xFFF87171))
                }
            }
        }
    }
}

@Composable
fun NotesCard(notes: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerLow)
            .padding(24.dp)
    ) {
        Text(
            text = "NOTES PERSONNELLES",
            style = MaterialTheme.typography.labelSmall,
            color = Primary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(notes, style = MaterialTheme.typography.bodyMedium, color = OnSurface, lineHeight = 24.sp)
    }
}

@Composable
fun MetadataChip(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainerLow.copy(alpha = 0.5f))
            .padding(12.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant, fontSize = 9.sp)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Bold)
    }
}
