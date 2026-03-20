package com.thevault.ui.screen.console

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
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.thevault.domain.model.Console
import com.thevault.ui.screen.gamedetail.ExternalLinksSection
import com.thevault.ui.screen.gamedetail.MetadataChip
import com.thevault.ui.theme.Primary
import com.thevault.ui.theme.Surface
import com.thevault.ui.theme.SurfaceContainer
import com.thevault.ui.theme.SurfaceContainerLow
import com.thevault.ui.theme.Tertiary
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ConsoleDetailScreen(
    consoleId: String,
    onBack: () -> Unit,
    viewModel: ConsoleDetailViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(consoleId) {
        viewModel.loadConsole(consoleId)
    }

    ConsoleDetailContent(uiState = uiState, onBack = onBack)
}

@Composable
fun ConsoleDetailContent(
    uiState: ConsoleDetailUiState,
    onBack: () -> Unit,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
            .verticalScroll(scrollState),
    ) {
        // Hero section
        Box(modifier = Modifier.fillMaxWidth().height(360.dp)) {
            if (uiState.console?.imageUrl != null) {
                AsyncImage(
                    model = uiState.console.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().blur(40.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded),
                    contentScale = ContentScale.Crop,
                    alpha = 0.3f,
                )
                Box(
                    modifier = Modifier
                        .padding(top = 70.dp)
                        .size(200.dp, 240.dp)
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.4f)),
                ) {
                    AsyncImage(
                        model = uiState.console.imageUrl,
                        contentDescription = uiState.console.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                }
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
            uiState.console?.let { console ->
                ConsoleInfoSection(console = console)
            }
        }
    }
}

@Composable
private fun ConsoleInfoSection(console: Console) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
    ) {
        Text(
            text = console.manufacturer.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = Tertiary,
            letterSpacing = 2.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = console.name,
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Black,
        )

        // Edition badges
        if (console.isCollectorEdition || console.isLimitedEdition) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (console.isCollectorEdition) {
                    EditionBadge(label = "Édition Collector", color = Color(0xFFFFD166))
                }
                if (console.isLimitedEdition) {
                    EditionBadge(label = "Édition Limitée", color = Tertiary)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MetadataChip(label = "STATUT", value = console.status.name, modifier = Modifier.weight(1f))
            MetadataChip(label = "RÉGION", value = console.region.name, modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MetadataChip(label = "ANNÉE", value = console.productionYear.toString(), modifier = Modifier.weight(1f))
            MetadataChip(label = "COULEUR", value = console.color, modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(12.dp))
        MetadataChip(label = "FORMAT", value = console.condition.name.replace("_", " "))

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
                text = "%.2f €".format(console.currentValue ?: 0.0),
                style = MaterialTheme.typography.headlineLarge,
                color = Primary,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Investi", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${console.purchasePrice ?: "—"} €", style = MaterialTheme.typography.titleMedium, color = Color.White)
                }
                val roi = if (console.currentValue != null && console.purchasePrice != null) {
                    requireNotNull(console.currentValue) - requireNotNull(console.purchasePrice)
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
            query = console.name,
            youtubeQuery = "${console.name} review",
        )

        // Personal notes
        val notes = console.notes
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
