package com.thevault.ui.screen.add

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.thevault.domain.model.ItemStatus
import com.thevault.ui.component.Eyebrow
import com.thevault.ui.component.ItemCover
import com.thevault.ui.component.PlatformBadge
import com.thevault.ui.component.SegmentedControl
import com.thevault.ui.component.VaultButton
import com.thevault.ui.component.VaultFilterChip
import com.thevault.ui.component.clickableNoRipple
import com.thevault.ui.component.formatLabel
import com.thevault.ui.component.label
import com.thevault.ui.component.shortPlatformName
import com.thevault.ui.screen.collection.CollectionEntry
import com.thevault.ui.screen.collection.ItemKind
import com.thevault.ui.screen.collection.owned
import com.thevault.ui.theme.Background
import com.thevault.ui.theme.CardBorderColor
import com.thevault.ui.theme.ModalShape
import com.thevault.ui.theme.OnPrimary
import com.thevault.ui.theme.OnSurface
import com.thevault.ui.theme.OnSurfaceMuted
import com.thevault.ui.theme.OnSurfaceVariant
import com.thevault.ui.theme.Outline
import com.thevault.ui.theme.Primary
import com.thevault.ui.theme.SurfaceContainerHigh
import com.thevault.ui.theme.SurfaceContainerLow
import com.thevault.ui.theme.VaultRadius

private val Destinations = listOf(ItemStatus.OWNED, ItemStatus.WISHLIST)

/**
 * Callbacks of the quick confirmation sheet.
 *
 * @property onFormChange Choice change callback.
 * @property onDismiss Closes the sheet.
 * @property onSave Saves; `true` opens the full form afterwards (« Ajouter des détails »).
 */
data class QuickAddActions(
    val onFormChange: (QuickAddForm) -> Unit,
    val onDismiss: () -> Unit,
    val onSave: (details: Boolean) -> Unit,
)

/**
 * Quick confirmation sheet (« Ajout · confirmer »).
 *
 * @param candidate Item being added.
 * @param form Current choices.
 * @param entries Collection entries (hints).
 * @param actions Sheet callbacks.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddSheet(
    candidate: AddCandidate,
    form: QuickAddForm,
    entries: List<CollectionEntry>,
    actions: QuickAddActions,
) {
    val onFormChange = actions.onFormChange
    ModalBottomSheet(
        onDismissRequest = actions.onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SurfaceContainerLow,
        shape = ModalShape,
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            SheetHeader(candidate, form.platform)
            val choosePlatform = candidate.platforms.size > 1 || candidate.kind != ItemKind.Game
            if (choosePlatform) PlatformChoice(candidate, form, onFormChange)
            SegmentedControl(Destinations, form.status, ::destinationLabel, onSelect = { onFormChange(form.copy(status = it)) })
            FormatAndRegion(form, cibShare(entries.owned()), onFormChange)
            PriceField(form.price) { onFormChange(form.copy(price = it)) }
            SaveActions(candidate.kind, form.status, actions.onSave)
        }
    }
}

private fun destinationLabel(status: ItemStatus) = if (status == ItemStatus.OWNED) "Collection" else "Wishlist"

@Composable
private fun FormatAndRegion(
    form: QuickAddForm,
    cib: Int?,
    onFormChange: (QuickAddForm) -> Unit,
) {
    val formatHint = cib?.let { "CIB pour $it\u202F% de ta collection" }
    val formats = OptionGroup("Format", formatHint, QuickFormats) { it.formatLabel }
    OptionRow(formats, form.condition) { onFormChange(form.copy(condition = it)) }
    val regions = OptionGroup("Région", "la plus fréquente", QuickRegions) { it.label }
    OptionRow(regions, form.region) { onFormChange(form.copy(region = it)) }
}

@Composable
private fun SaveActions(
    kind: ItemKind,
    status: ItemStatus,
    onSave: (details: Boolean) -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        val label = if (status == ItemStatus.OWNED) "Ajouter à la collection" else "Ajouter à la wishlist"
        VaultButton(label, { onSave(false) }, Modifier.fillMaxWidth())
        if (kind == ItemKind.Game) {
            Text(
                "Ajouter des détails : état, édition, notes",
                style = MaterialTheme.typography.titleMedium,
                color = Primary,
                modifier = Modifier.align(Alignment.CenterHorizontally).clickableNoRipple { onSave(true) },
            )
        }
    }
}

/** Options of a single-choice row: title, optional hint and labelled options. */
private data class OptionGroup<T>(
    val title: String,
    val hint: String?,
    val options: List<T>,
    val label: (T) -> String,
)

@Composable
private fun SheetHeader(
    candidate: AddCandidate,
    platform: String,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        ItemCover(candidate.remote?.coverUrl, platform, candidate.title, Modifier.size(58.dp, 76.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(candidate.title, style = MaterialTheme.typography.titleLarge, color = OnSurface, maxLines = 2)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                PlatformBadge(platform)
                if (candidate.subtitle.isNotBlank()) {
                    Text(
                        candidate.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlatformChoice(
    candidate: AddCandidate,
    form: QuickAddForm,
    onFormChange: (QuickAddForm) -> Unit,
) {
    if (candidate.platforms.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Eyebrow(if (candidate.kind == ItemKind.Accessory) "Compatible avec" else "Plateforme")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(candidate.platforms) { p ->
                VaultFilterChip(shortPlatformName(p), form.platform == p, { onFormChange(form.copy(platform = p)) })
            }
        }
    }
}

@Composable
private fun <T> OptionRow(
    group: OptionGroup<T>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Eyebrow(group.title, Modifier.weight(1f))
            group.hint?.let { hint -> Text(hint, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            group.options.forEach { option ->
                val on = option == selected
                Box(
                    Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(VaultRadius.lg)
                        .background(if (on) Primary else SurfaceContainerHigh)
                        .then(if (on) Modifier else Modifier.border(1.dp, Outline, VaultRadius.lg))
                        .clickable { onSelect(option) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(group.label(option), style = MaterialTheme.typography.labelLarge, color = if (on) OnPrimary else OnSurface)
                }
            }
        }
    }
}

@Composable
private fun PriceField(
    price: String,
    onChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Eyebrow("Prix payé · facultatif")
        BasicTextField(
            value = price,
            onValueChange = { onChange(it.filter { c -> c.isDigit() || c == ',' || c == '.' }) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle = MaterialTheme.typography.headlineSmall.copy(color = OnSurface),
            cursorBrush = SolidColor(Primary),
            decorationBox = { inner ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(VaultRadius.button)
                        .background(Background)
                        .border(1.dp, CardBorderColor, VaultRadius.button)
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.weight(1f)) {
                        if (price.isEmpty()) Text("0,00", style = MaterialTheme.typography.headlineSmall, color = OnSurfaceMuted)
                        inner()
                    }
                    Text("€", style = MaterialTheme.typography.headlineSmall, color = OnSurfaceVariant)
                }
            },
        )
    }
}
