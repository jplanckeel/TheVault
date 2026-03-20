package com.thevault.ui.screen.accessory

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.thevault.ui.theme.Primary
import com.thevault.ui.theme.Surface
import com.thevault.ui.theme.SurfaceContainerLow
import com.thevault.ui.theme.Tertiary
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AccessoryScreen(
    onAccessoryClick: (String) -> Unit = {},
    viewModel: AccessoryViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    AccessoryContent(uiState = uiState, onAccessoryClick = onAccessoryClick)
}

@Composable
fun AccessoryContent(
    uiState: AccessoryUiState,
    onAccessoryClick: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
            .padding(top = 24.dp),
    ) {
        Text(
            text = "MES ACCESSOIRES",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp),
            letterSpacing = 2.sp,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Périphériques",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        } else if (uiState.accessories.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Aucun accessoire dans la collection.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(uiState.accessories) { accessory ->
                    AccessoryCard(
                        accessory = accessory,
                        onClick = { onAccessoryClick(accessory.id) },
                    )
                }
            }
        }
    }
}

@Composable
fun AccessoryCard(
    accessory: Accessory,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainerLow)
            .clickable { onClick() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Accessory image or placeholder
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
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
                Text(text = "🔧", fontSize = 28.sp)
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = accessory.type.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp,
                fontSize = 10.sp,
            )
            Text(
                text = accessory.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = accessory.compatibleConsole,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // Edition badges
            if (accessory.isCollectorEdition || accessory.isLimitedEdition) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (accessory.isCollectorEdition) {
                        MiniEditionBadge(label = "Collector", color = Color(0xFFFFD166))
                    }
                    if (accessory.isLimitedEdition) {
                        MiniEditionBadge(label = "Limité", color = Tertiary)
                    }
                }
            }
        }

        Text(
            text = "%.0f €".format(accessory.currentValue ?: 0.0),
            style = MaterialTheme.typography.titleMedium,
            color = Primary,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
private fun MiniEditionBadge(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
