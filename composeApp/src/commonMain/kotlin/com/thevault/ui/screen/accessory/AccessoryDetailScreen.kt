package com.thevault.ui.screen.accessory

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.thevault.domain.model.Accessory
import com.thevault.ui.screen.gamedetail.ExternalLinksSection
import com.thevault.ui.screen.gamedetail.MetadataChip
import com.thevault.ui.theme.Primary
import com.thevault.ui.theme.Surface
import com.thevault.ui.theme.SurfaceContainer
import com.thevault.ui.theme.SurfaceContainerLow
import com.thevault.ui.theme.Tertiary
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AccessoryDetailScreen(
    accessoryId: String,
    onBack: () -> Unit,
    viewModel: AccessoryDetailViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(accessoryId) {
        viewModel.loadAccessory(accessoryId)
    }

    AccessoryDetailContent(uiState = uiState, onBack = onBack)
}

@Composable
fun AccessoryDetailContent(
    uiState: AccessoryDetailUiState,
    onBack: () -> Unit,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
            .verticalScroll(scrollState),
    ) {
        // Top bar with image placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(SurfaceContainer),
        ) {
            if (uiState.accessory?.imageUrl != null) {
                AsyncImage(
                    model = uiState.accessory.imageUrl,
                    contentDescription = uiState.accessory.name,
                    modifier = Modifier
                        .size(180.dp)
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Fit,
                )
            } else {
                Text(
                    text = "🔧",
                    fontSize = 72.sp,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            IconButton(
                onClick = onBack,
                modifier = Modifier.padding(16.dp).align(Alignment.TopStart),
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Retour", tint = Color.White)
            }
        }

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        } else {
            uiState.accessory?.let { accessory ->
                AccessoryDetailInfoSection(accessory = accessory)
            }
        }
    }
}

@Composable
private fun AccessoryDetailInfoSection(accessory: Accessory) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
    ) {
        Text(
            text = accessory.type.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF63EFEC),
            letterSpacing = 2.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = accessory.name,
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Black,
        )

        // Edition badges
        if (accessory.isCollectorEdition || accessory.isLimitedEdition) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (accessory.isCollectorEdition) {
                    EditionBadge(label = "Édition Collector", color = Color(0xFFFFD166))
                }
                if (accessory.isLimitedEdition) {
                    EditionBadge(label = "Édition Limitée", color = Tertiary)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MetadataChip(label = "STATUT", value = accessory.status.name, modifier = Modifier.weight(1f))
            MetadataChip(label = "TYPE", value = accessory.type, modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(12.dp))
        MetadataChip(
            label = "CONSOLE COMPATIBLE",
            value = accessory.compatibleConsole,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))
        MetadataChip(
            label = "FORMAT",
            value = accessory.condition.name.replace("_", " "),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Financial card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceContainerLow)
                .padding(20.dp),
        ) {
            Text("VALEUR ESTIMÉE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = "%.2f €".format(accessory.currentValue ?: 0.0),
                style = MaterialTheme.typography.headlineLarge,
                color = Primary,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Investi", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${accessory.purchasePrice ?: "—"} €", style = MaterialTheme.typography.titleMedium, color = Color.White)
                }
                val roi = if (accessory.currentValue != null && accessory.purchasePrice != null) {
                    requireNotNull(accessory.currentValue) - requireNotNull(accessory.purchasePrice)
                } else null
                if (roi != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("ROI", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${if (roi >= 0) "+" else ""}%.2f €".format(roi),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (roi >= 0) Color(0xFF4ADE80) else Color(0xFFF87171),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // External links
        ExternalLinksSection(
            query = "${accessory.name} ${accessory.compatibleConsole}",
            youtubeQuery = "${accessory.name} ${accessory.compatibleConsole} review",
        )

        // Personal notes
        val notes = accessory.notes
        if (!notes.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(24.dp))
            Text("NOTES PERSONNELLES", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Text(notes, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        }

        Spacer(modifier = Modifier.height(64.dp))
    }
}

@Composable
private fun EditionBadge(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
        )
    }
}
