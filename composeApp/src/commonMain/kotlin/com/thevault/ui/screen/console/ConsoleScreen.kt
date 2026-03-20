package com.thevault.ui.screen.console

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
import com.thevault.domain.model.Console
import com.thevault.ui.theme.Primary
import com.thevault.ui.theme.Surface
import com.thevault.ui.theme.SurfaceContainerLow
import com.thevault.ui.theme.Tertiary
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ConsoleScreen(
    onConsoleClick: (String) -> Unit = {},
    viewModel: ConsoleViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    ConsoleContent(uiState = uiState, onConsoleClick = onConsoleClick)
}

@Composable
fun ConsoleContent(
    uiState: ConsoleUiState,
    onConsoleClick: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
            .padding(top = 24.dp),
    ) {
        Text(
            text = "MES CONSOLES",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp),
            letterSpacing = 2.sp,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Hardware",
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
        } else if (uiState.consoles.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Aucune console dans la collection.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(uiState.consoles) { console ->
                    ConsoleCard(
                        console = console,
                        onClick = { onConsoleClick(console.id) },
                    )
                }
            }
        }
    }
}

@Composable
fun ConsoleCard(
    console: Console,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainerLow)
            .clickable { onClick() }
            .padding(16.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            if (console.imageUrl != null) {
                AsyncImage(
                    model = console.imageUrl,
                    contentDescription = console.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            }

            // Edition badges overlay
            if (console.isCollectorEdition || console.isLimitedEdition) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (console.isCollectorEdition) {
                        ConsoleMiniEditionBadge(label = "Collector", color = Color(0xFFFFD166))
                    }
                    if (console.isLimitedEdition) {
                        ConsoleMiniEditionBadge(label = "Limité", color = Tertiary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = console.manufacturer.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                )
                Text(
                    text = console.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = "%.0f €".format(console.currentValue ?: 0.0),
                style = MaterialTheme.typography.titleLarge,
                color = Tertiary,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun ConsoleMiniEditionBadge(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.85f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Black,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
