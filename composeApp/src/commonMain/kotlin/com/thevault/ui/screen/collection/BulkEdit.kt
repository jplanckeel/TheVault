package com.thevault.ui.screen.collection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thevault.domain.model.CopyDetails
import com.thevault.ui.component.ButtonVariant
import com.thevault.ui.component.Engrave
import com.thevault.ui.component.VaultButton
import com.thevault.ui.component.VaultChip
import com.thevault.ui.component.VaultTextField
import com.thevault.ui.screen.copy.RegionVariants
import com.thevault.ui.theme.Vault
import com.thevault.ui.theme.VaultRadius

/**
 * Change applied to several copies at once; blank fields are left untouched.
 *
 * @property variant Region variant to set.
 * @property location Storage place to set.
 * @property tags Tags to add (comma separated).
 */
data class BulkChange(
    val variant: String = "",
    val location: String = "",
    val tags: String = "",
) {
    /** Whether the change does something. */
    val isEmpty: Boolean get() = variant.isBlank() && location.isBlank() && tags.isBlank()

    /** Applies the change to [details]. */
    fun applyTo(details: CopyDetails): CopyDetails {
        val added = tags.split(',', ';').map { it.trim() }.filter { it.isNotEmpty() }
        return details.copy(
            regionVariant = variant.trim().uppercase().ifBlank { details.regionVariant },
            location = location.trim().ifBlank { details.location },
            tags = (details.tags + added).distinct(),
        )
    }
}

/**
 * Bulk edit sheet: region variant, storage place and tags for the selected copies.
 *
 * @param count Number of selected copies.
 * @param onApply Applies the change.
 * @param onDismiss Closes the sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkEditSheet(
    count: Int,
    onApply: (BulkChange) -> Unit,
    onDismiss: () -> Unit,
) {
    var change by rememberSaveable(stateSaver = BulkSaver) { mutableStateOf(BulkChange()) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Vault.colors.panel,
        scrimColor = Vault.colors.scrim,
        shape = VaultRadius.sheet,
    ) {
        Column(
            Modifier.padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Modifier $count objets", style = Vault.type.displayM.copy(fontSize = 20.sp), color = Vault.colors.ink)
            Engrave("Variante PAL")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(RegionVariants) { v ->
                    VaultChip(v, change.variant == v, {
                        change =
                            change.copy(variant = if (change.variant == v) "" else v)
                    })
                }
            }
            VaultTextField("Emplacement", change.location, { change = change.copy(location = it) }, placeholder = "Inchangé si vide")
            VaultTextField("Ajouter des étiquettes", change.tags, { change = change.copy(tags = it) }, placeholder = "rpg, à finir")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                VaultButton("Annuler", onDismiss, Modifier.weight(1f), ButtonVariant.Secondary)
                VaultButton("Appliquer", { onApply(change) }, Modifier.weight(1.4f), enabled = !change.isEmpty)
            }
        }
    }
}

private val BulkSaver =
    androidx.compose.runtime.saveable.Saver<BulkChange, List<String>>(
        save = { listOf(it.variant, it.location, it.tags) },
        restore = { BulkChange(it[0], it[1], it[2]) },
    )
