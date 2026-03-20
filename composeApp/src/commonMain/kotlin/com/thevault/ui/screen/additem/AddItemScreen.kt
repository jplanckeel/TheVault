package com.thevault.ui.screen.additem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.thevault.domain.model.RemoteGame
import com.thevault.ui.theme.*
import com.thevault.ui.theme.Surface as VaultSurface
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemScreen(
    gameId: String? = null,
    onBack: () -> Unit,
    viewModel: AddItemViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(gameId) {
        if (gameId != null) {
            viewModel.loadGame(gameId)
        }
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onBack()
        }
    }

    Scaffold(
        containerColor = VaultSurface,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = if (gameId != null) "MODIFIER UN ITEM" else "AJOUTER UN ITEM", 
                        style = MaterialTheme.typography.labelMedium, 
                        letterSpacing = 2.sp
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = VaultSurface,
                    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = if (gameId != null) "Mise à jour" else "Nouvelle Acquisition",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Black,
                )
                Spacer(modifier = Modifier.height(32.dp))

                // Title Search Section
                VaultTextField(
                    label = "TITRE DU JEU", 
                    value = uiState.title, 
                    onValueChange = viewModel::onTitleChange,
                    trailingIcon = {
                        if (uiState.isSearching) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Primary)
                        } else {
                            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        }
                    }
                )
            }

            // Vignettes Selection (Horizontal Gallery)
            if (uiState.searchResults.isNotEmpty()) {
                Text(
                    text = "RÉSULTATS IGDB",
                    style = MaterialTheme.typography.labelSmall,
                    color = Primary,
                    modifier = Modifier.padding(horizontal = 24.dp),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    uiState.searchResults.forEach { result ->
                        GameVignette(result) { viewModel.selectGame(result) }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                
                if (uiState.availablePlatforms.isNotEmpty()) {
                    Text(text = "PLATEFORME", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    PlatformSelector(
                        platforms = uiState.availablePlatforms,
                        selectedPlatform = uiState.platform,
                        onPlatformSelected = viewModel::onPlatformChange
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                } else {
                    VaultTextField(label = "PLATEFORME", value = uiState.platform, onValueChange = viewModel::onPlatformChange)
                    Spacer(modifier = Modifier.height(24.dp))
                }

                if (uiState.developer.isNotBlank() || uiState.gameModes.isNotBlank()) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        if (uiState.developer.isNotBlank()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("DÉVELOPPEUR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                                Text(uiState.developer, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        if (uiState.publisher.isNotBlank()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("ÉDITEUR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                                Text(uiState.publisher, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    if (uiState.gameModes.isNotBlank()) {
                        Text("MODES", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                        Text(uiState.gameModes, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    if (uiState.themes.isNotBlank()) {
                        Text("THÈMES", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                        Text(uiState.themes, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                VaultTextField(label = "PRIX D'ACHAT (€)", value = uiState.purchasePrice, onValueChange = viewModel::onPriceChange, keyboardType = KeyboardType.Number)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(text = "ÉTAT / FORMAT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                Spacer(modifier = Modifier.height(12.dp))
                CompletenessSelector(
                    selectedCondition = uiState.condition,
                    onConditionSelected = viewModel::onConditionChange
                )

                Spacer(modifier = Modifier.height(24.dp))
                
                Text(text = "ÉTAT PHYSIQUE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                Spacer(modifier = Modifier.height(8.dp))
                StarRatingSelector(
                    rating = uiState.rating,
                    onRatingChanged = viewModel::onRatingChange
                )

                Spacer(modifier = Modifier.height(48.dp))

                // CTA
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(PrimaryDim, Tertiary),
                            )
                        )
                ) {
                    Button(
                        onClick = viewModel::saveItem,
                        modifier = Modifier.fillMaxSize(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        enabled = !uiState.isSaving && uiState.title.isNotBlank()
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(color = OnPrimaryFixed, modifier = Modifier.size(24.dp))
                        } else {
                            Text("SCELLER DANS LE VAULT", color = OnPrimaryFixed, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(64.dp))
            }
        }
    }
}

@Composable
fun GameVignette(
    game: RemoteGame,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceContainerLowest)
        ) {
            if (game.coverUrl != null) {
                AsyncImage(
                    model = game.coverUrl,
                    contentDescription = game.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.Search, 
                    contentDescription = null, 
                    modifier = Modifier.align(Alignment.Center).size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = game.title,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = game.platformNames.firstOrNull() ?: "Platform",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
fun VaultTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Column(modifier = modifier) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = SurfaceContainerLowest,
                unfocusedContainerColor = SurfaceContainerLowest,
                disabledContainerColor = SurfaceContainerLowest,
                focusedIndicatorColor = Primary,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            trailingIcon = trailingIcon
        )
    }
}

@Composable
fun CompletenessSelector(
    selectedCondition: com.thevault.domain.model.ItemCondition,
    onConditionSelected: (com.thevault.domain.model.ItemCondition) -> Unit
) {
    val formats = listOf(
        com.thevault.domain.model.ItemCondition.LOOSE to "En loose",
        com.thevault.domain.model.ItemCondition.CIB_NO_MANUAL to "Boîte sans notice",
        com.thevault.domain.model.ItemCondition.CIB to "Complet (CIB)",
        com.thevault.domain.model.ItemCondition.NEW_SEALED to "Neuf (Scellé)"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        formats.forEach { (condition, label) ->
            val isSelected = selectedCondition == condition
            val bgColor = if (isSelected) Primary.copy(alpha = 0.2f) else SurfaceContainerLowest
            val borderColor = if (isSelected) Primary else Color.Transparent
            val textColor = if (isSelected) Primary else MaterialTheme.colorScheme.onSurfaceVariant

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgColor)
                    .clickable { onConditionSelected(condition) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun StarRatingSelector(
    rating: Int,
    onRatingChanged: (Int) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (i in 1..5) {
            val isFilled = i <= rating
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isFilled) Primary.copy(alpha = 0.2f) else SurfaceContainerLowest)
                    .clickable { onRatingChanged(i) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Star,
                    contentDescription = "$i Etoiles",
                    modifier = Modifier.size(24.dp),
                    tint = if (isFilled) Primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun PlatformSelector(
    platforms: List<String>,
    selectedPlatform: String,
    onPlatformSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        platforms.forEach { platform ->
            val isSelected = selectedPlatform == platform
            val bgColor = if (isSelected) Primary.copy(alpha = 0.2f) else SurfaceContainerLowest
            val textColor = if (isSelected) Primary else MaterialTheme.colorScheme.onSurfaceVariant

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgColor)
                    .clickable { onPlatformSelected(platform) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = platform,
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
