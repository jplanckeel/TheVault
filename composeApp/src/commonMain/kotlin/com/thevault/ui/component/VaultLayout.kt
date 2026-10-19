package com.thevault.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thevault.ui.theme.CardBorderColor
import com.thevault.ui.theme.OnSurface
import com.thevault.ui.theme.OnSurfaceVariant
import com.thevault.ui.theme.Primary
import com.thevault.ui.theme.SurfaceContainerHigh
import com.thevault.ui.theme.TabularNumbers
import com.thevault.ui.theme.VaultRadius

/**
 * Root screen header — big Montserrat title and an optional counter line,
 * like "My Collection / 1 248 items" on the web.
 *
 * @param title Screen title.
 * @param modifier Modifier applied to the header.
 * @param subtitle Optional secondary line.
 * @param action Optional trailing action (icon button…).
 */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: @Composable (() -> Unit)? = null,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.displayLarge, color = OnSurface)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium.merge(TabularNumbers), color = OnSurfaceVariant)
            }
        }
        action?.invoke()
    }
}

/**
 * Section title with an optional text action on the right ("Voir tout").
 *
 * @param title Section title.
 * @param modifier Modifier applied to the header.
 * @param actionLabel Optional action label.
 * @param onAction Action click handler.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = OnSurface, modifier = Modifier.weight(1f))
        if (actionLabel != null) {
            Text(
                actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = Primary,
                modifier = Modifier.clip(VaultRadius.md).clickable(onClick = onAction).padding(8.dp),
            )
        }
    }
}

/**
 * Empty state — icon in a circle, title, text and optional call to action.
 *
 * @param icon Illustration icon.
 * @param title Main message.
 * @param modifier Modifier applied to the container.
 * @param message Optional explanation.
 * @param actionLabel Optional CTA label.
 * @param onAction CTA click handler.
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(80.dp).clip(VaultRadius.full).background(SurfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(40.dp))
        }
        Text(title, style = MaterialTheme.typography.headlineSmall, color = OnSurface, textAlign = TextAlign.Center)
        if (message != null) {
            Text(message, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant, textAlign = TextAlign.Center)
        }
        if (actionLabel != null) {
            VaultButton(text = actionLabel, onClick = onAction, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

/**
 * Search field — `surface-high`, 12dp radius, leading search icon, clear button.
 *
 * @param query Current query.
 * @param onQueryChange Query change callback.
 * @param placeholder Placeholder text.
 * @param modifier Modifier applied to the field.
 */
@Composable
fun VaultSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = OnSurface),
        cursorBrush = SolidColor(Primary),
        modifier = modifier,
        decorationBox = { inner ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(VaultRadius.lg)
                    .background(SurfaceContainerHigh)
                    .border(1.dp, CardBorderColor, VaultRadius.lg)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(20.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant.copy(alpha = 0.7f))
                    }
                    inner()
                }
                if (query.isNotEmpty()) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Effacer",
                        tint = OnSurfaceVariant,
                        modifier = Modifier.size(20.dp).clip(VaultRadius.full).clickable { onQueryChange("") },
                    )
                }
            }
        },
    )
}
