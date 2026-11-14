@file:Suppress("MagicNumber") // Design tokens and geometry of the console mockups.

package com.thevault.ui.screen.add

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thevault.domain.model.ItemStatus
import com.thevault.ui.component.ButtonVariant
import com.thevault.ui.component.CoverSize
import com.thevault.ui.component.Engrave
import com.thevault.ui.component.GameCover
import com.thevault.ui.component.LinkText
import com.thevault.ui.component.PlatformTag
import com.thevault.ui.component.Segment
import com.thevault.ui.component.SegmentedControl
import com.thevault.ui.component.SourceBadge
import com.thevault.ui.component.VaultButton
import com.thevault.ui.component.VaultChip
import com.thevault.ui.component.VaultTextField
import com.thevault.ui.component.formatLabel
import com.thevault.ui.component.label
import com.thevault.ui.component.shortPlatformName
import com.thevault.ui.screen.collection.ItemKind
import com.thevault.ui.theme.Vault
import com.thevault.ui.theme.VaultRadius

/**
 * Callbacks of the quick confirmation sheet.
 *
 * @property onFormChange Choice change callback.
 * @property onDismiss Closes the sheet.
 * @property onSave Saves with a status; `true` opens the full form afterwards.
 */
data class QuickAddActions(
    val onFormChange: (QuickAddForm) -> Unit,
    val onDismiss: () -> Unit,
    val onSave: (status: ItemStatus, details: Boolean) -> Unit,
)

/**
 * Quick confirmation sheet: platform, condition, region, price, then « Envie » or « Ajouter ».
 *
 * @param candidate Item being added.
 * @param form Current choices.
 * @param actions Sheet callbacks.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddSheet(
    candidate: AddCandidate,
    form: QuickAddForm,
    actions: QuickAddActions,
) {
    val c = Vault.colors
    ModalBottomSheet(
        onDismissRequest = actions.onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.panel,
        scrimColor = c.scrim,
        shape = VaultRadius.sheet,
        dragHandle = { SheetGrip() },
    ) {
        Column(
            Modifier
                .verticalScroll(
                    rememberScrollState(),
                ).padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(candidate.title, style = Vault.type.displayM.copy(fontSize = 20.sp), color = c.ink)
            SheetHeader(candidate, form)
            if (candidate.kind == ItemKind.Accessory ||
                (candidate.kind == ItemKind.Game && candidate.platforms.size > 1)
            ) {
                PlatformChoice(candidate, form, actions.onFormChange)
            }
            ConditionAndRegion(form, actions.onFormChange)
            VaultTextField(
                "Prix payé",
                form.price,
                { actions.onFormChange(form.copy(price = it)) },
                placeholder = "Facultatif",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                suffix = "€",
            )
            SaveButtons(candidate, actions)
        }
    }
}

@Composable
private fun ConditionAndRegion(
    form: QuickAddForm,
    onFormChange: (QuickAddForm) -> Unit,
) {
    Engrave("État")
    SegmentedControl(QuickFormats.map { Segment(it.formatLabel) }, QuickFormats.indexOf(form.condition).coerceAtLeast(0), {
        onFormChange(form.copy(condition = QuickFormats[it]))
    })
    Engrave("Région")
    SegmentedControl(QuickRegions.map { Segment(it.label) }, QuickRegions.indexOf(form.region).coerceAtLeast(0), {
        onFormChange(form.copy(region = QuickRegions[it]))
    })
}

@Composable
private fun ColumnScope.SaveButtons(
    candidate: AddCandidate,
    actions: QuickAddActions,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        VaultButton("Envie", {
            actions.onSave(ItemStatus.WISHLIST, false)
        }, Modifier.weight(1f), ButtonVariant.Secondary, icon = Icons.Default.FavoriteBorder)
        VaultButton("Ajouter", { actions.onSave(ItemStatus.OWNED, false) }, Modifier.weight(1.6f), icon = Icons.Default.Inventory2)
    }
    if (candidate.kind == ItemKind.Game) {
        LinkText(
            "Ajouter puis compléter la fiche",
            { actions.onSave(ItemStatus.OWNED, true) },
            Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

@Composable
private fun SheetGrip() {
    Box(
        Modifier
            .padding(top = 10.dp)
            .size(36.dp, 5.dp)
            .clip(VaultRadius.pill)
            .background(Vault.colors.line),
    )
}

@Composable
private fun SheetHeader(
    candidate: AddCandidate,
    form: QuickAddForm,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        GameCover(candidate.remote?.coverUrl, candidate.title, form.platform, Modifier.width(84.dp), CoverSize.M)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PlatformTag(form.platform.ifBlank { candidate.kind.singular }, region = form.region)
            val genre = candidate.remote?.genres?.firstOrNull()
            val line = listOfNotNull(candidate.subtitle.ifBlank { null }, genre).joinToString(" · ")
            if (line.isNotBlank()) Text(line, style = Vault.type.caption, color = Vault.colors.inkMuted)
            if (candidate.remote != null) SourceBadge("Fiche remplie")
        }
    }
}

@Composable
private fun PlatformChoice(
    candidate: AddCandidate,
    form: QuickAddForm,
    onFormChange: (QuickAddForm) -> Unit,
) {
    Engrave(if (candidate.kind == ItemKind.Accessory) "Console compatible" else "Plateforme")
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(candidate.platforms) { p -> VaultChip(shortPlatformName(p), form.platform == p, { onFormChange(form.copy(platform = p)) }) }
    }
}
