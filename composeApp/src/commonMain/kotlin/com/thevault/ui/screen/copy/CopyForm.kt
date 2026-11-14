package com.thevault.ui.screen.copy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.thevault.domain.model.Edition
import com.thevault.domain.model.Grade
import com.thevault.domain.model.ItemStatus
import com.thevault.ui.component.ConditionTag
import com.thevault.ui.component.Engrave
import com.thevault.ui.component.Panel
import com.thevault.ui.component.VaultChip
import com.thevault.ui.component.VaultTextField
import com.thevault.ui.component.label
import com.thevault.ui.theme.Vault

private val Decimal = KeyboardOptions(keyboardType = KeyboardType.Decimal)
private val DateKeys = KeyboardOptions(keyboardType = KeyboardType.Number)

/**
 * Every field of a copy: parts and grades, edition, region variant, purchase, storage,
 * tags, and loan / sale details when the status calls for them.
 *
 * @param draft Current values.
 * @param status Status of the item (shows the loan or sale fields).
 * @param onChange Called with the edited draft.
 */
@Composable
fun CopyFormSections(
    draft: CopyDraft,
    status: ItemStatus,
    onChange: (CopyDraft) -> Unit,
) {
    PartsSection(draft, onChange)
    EditionSection(draft, onChange)
    PurchaseSection(draft, onChange)
    StorageSection(draft, onChange)
    if (status == ItemStatus.LOANED) LoanSection(draft, onChange)
    if (status == ItemStatus.SOLD) SaleSection(draft, onChange)
    if (!draft.datesValid) Text("Dates au format JJ/MM/AAAA.", style = Vault.type.caption, color = Vault.colors.loss)
}

@Composable
private fun PartsSection(
    draft: CopyDraft,
    onChange: (CopyDraft) -> Unit,
) {
    val c = draft.components
    Panel(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row {
                Engrave("Exemplaire", Modifier.weight(1f))
                ConditionTag(c)
            }
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    "media" to "Support",
                    "box" to "Boîte",
                    "manual" to "Notice",
                    "extras" to "Extras",
                    "sealed" to "Scellé",
                    "digital" to "Démat.",
                ).forEach { (key, label) ->
                    val on =
                        when (key) {
                            "media" -> c.media
                            "box" -> c.box
                            "manual" -> c.manual
                            "extras" -> c.extras
                            "sealed" -> c.sealed
                            else -> c.digital
                        }
                    VaultChip(label, on, { onChange(draft.copy(components = c.toggle(key))) })
                }
            }
            if (c.media) GradeRow("État du support", c.mediaGrade) { onChange(draft.copy(components = c.copy(mediaGrade = it))) }
            if (c.box) GradeRow("État de la boîte", c.boxGrade) { onChange(draft.copy(components = c.copy(boxGrade = it))) }
            if (c.manual) GradeRow("État de la notice", c.manualGrade) { onChange(draft.copy(components = c.copy(manualGrade = it))) }
        }
    }
}

@Composable
private fun GradeRow(
    label: String,
    grade: Grade?,
    onSelect: (Grade?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Engrave(label)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(Grade.entries) { g -> VaultChip(g.label, g == grade, { onSelect(if (g == grade) null else g) }) }
        }
    }
}

@Composable
private fun EditionSection(
    draft: CopyDraft,
    onChange: (CopyDraft) -> Unit,
) {
    Engrave("Édition")
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(Edition.entries) { e -> VaultChip(e.label, e == draft.edition, { onChange(draft.copy(edition = e)) }) }
    }
    Engrave("Variante PAL")
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(RegionVariants) { v ->
            val on = draft.regionVariant.equals(v, ignoreCase = true)
            VaultChip(v, on, { onChange(draft.copy(regionVariant = if (on) "" else v)) })
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        VaultTextField("Autre variante", draft.regionVariant, { onChange(draft.copy(regionVariant = it)) }, Modifier.weight(1f))
        VaultTextField("Code produit", draft.productCode, {
            onChange(draft.copy(productCode = it))
        }, Modifier.weight(1.4f), placeholder = "DOL-GZLP-FAH")
    }
}

@Composable
private fun PurchaseSection(
    draft: CopyDraft,
    onChange: (CopyDraft) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        VaultTextField(
            "Prix payé",
            draft.price,
            { onChange(draft.copy(price = it)) },
            Modifier.weight(1f),
            keyboardOptions = Decimal,
            suffix = "€",
        )
        VaultTextField("Port", draft.shipping, {
            onChange(draft.copy(shipping = it))
        }, Modifier.weight(1f), keyboardOptions = Decimal, suffix = "€")
    }
    VaultTextField("Date d'achat", draft.acquiredAt, {
        onChange(draft.copy(acquiredAt = it))
    }, placeholder = "JJ/MM/AAAA", keyboardOptions = DateKeys)
    Engrave("Acheté chez")
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(PurchaseSources) { s ->
            val on = draft.acquiredFrom.equals(s, ignoreCase = true)
            VaultChip(s, on, { onChange(draft.copy(acquiredFrom = if (on) "" else s)) })
        }
    }
    VaultTextField("Source", draft.acquiredFrom, { onChange(draft.copy(acquiredFrom = it)) }, placeholder = "Salon, échange…")
}

@Composable
private fun StorageSection(
    draft: CopyDraft,
    onChange: (CopyDraft) -> Unit,
) {
    VaultTextField("Emplacement", draft.location, { onChange(draft.copy(location = it)) }, placeholder = "Étagère 2, carton bleu…")
    VaultTextField("Étiquettes", draft.tags, { onChange(draft.copy(tags = it)) }, placeholder = "rpg, à finir, à revendre")
}

@Composable
private fun LoanSection(
    draft: CopyDraft,
    onChange: (CopyDraft) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        VaultTextField("Prêté à", draft.loanedTo, { onChange(draft.copy(loanedTo = it)) }, Modifier.weight(1.3f))
        VaultTextField(
            "Retour prévu",
            draft.loanDueAt,
            { onChange(draft.copy(loanDueAt = it)) },
            Modifier.weight(1f),
            "JJ/MM/AAAA",
            DateKeys,
        )
    }
}

@Composable
private fun SaleSection(
    draft: CopyDraft,
    onChange: (CopyDraft) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        VaultTextField("Vendu", draft.soldPrice, {
            onChange(draft.copy(soldPrice = it))
        }, Modifier.weight(1f), keyboardOptions = Decimal, suffix = "€")
        VaultTextField("Le", draft.soldAt, { onChange(draft.copy(soldAt = it)) }, Modifier.weight(1f), "JJ/MM/AAAA", DateKeys)
    }
}
