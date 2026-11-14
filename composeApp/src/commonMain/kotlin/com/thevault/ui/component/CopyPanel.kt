package com.thevault.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.thevault.domain.model.CopyDetails
import com.thevault.domain.model.Edition
import com.thevault.ui.theme.Vault
import com.thevault.ui.theme.VaultRadius
import kotlinx.datetime.LocalDate
import kotlin.math.abs

/**
 * Purchase of a copy, for the « Mon exemplaire » panel.
 *
 * @property price Price paid.
 * @property date Purchase date.
 * @property value Effective value.
 * @property indicative Whether [value] is only the indicative US price of an import.
 */
data class CopyPurchase(
    val price: Double?,
    val date: LocalDate?,
    val value: Double?,
    val indicative: Boolean = false,
)

/**
 * « Mon exemplaire » panel: paid (+ shipping), quote, latent gain, parts with their wear,
 * purchase line, storage, tags, loan and sale.
 *
 * @param details Copy details.
 * @param purchase Price, date and value.
 */
@Composable
fun CopyPanel(
    details: CopyDetails,
    purchase: CopyPurchase,
) {
    val c = Vault.colors
    Panel(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Engrave("Mon exemplaire", Modifier.weight(1f))
            ConditionTag(details.components)
        }
        CopyFigures(details.totalCost(purchase.price), purchase.value, details.shippingCost, purchase.indicative)
        if (purchase.indicative) {
            Text(
                "Cote US indicative (import Gameye) : pas encore de cote PAL.",
                style = Vault.type.caption,
                color = c.inkMuted,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        details.components.parts.takeIf { it.isNotEmpty() }?.let { parts ->
            Box(
                Modifier
                    .padding(top = 14.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(c.line),
            )
            FlowRow(
                Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                parts.forEach { Part(it) }
            }
        }
        val line =
            listOfNotNull(
                purchase.date?.let { "Acheté le ${formatLongDate(it)}" },
                details.acquiredFrom,
                details.edition.takeIf { it != Edition.STANDARD }?.label,
            )
        if (line.isNotEmpty()) {
            Text(
                line.joinToString(" · ").uppercase(),
                style = Vault.type.monoTag,
                color = c.inkMuted,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        CopyFacts(details)
    }
}

@Composable
private fun CopyFacts(details: CopyDetails) {
    val c = Vault.colors
    val facts =
        listOfNotNull(
            details.location?.let { "Rangé : $it" },
            details.loanedTo?.let { who -> "Prêté à $who" + (details.loanDueAt?.let { " · retour le ${formatLongDate(it)}" } ?: "") },
            details.soldPrice?.let { p ->
                "Vendu ${formatEuro(p, decimals = 2)}" +
                    (details.soldAt?.let { " le ${formatLongDate(it)}" } ?: "")
            },
            details.productCode?.let { "Code $it" },
        )
    facts.forEach { Text(it, style = Vault.type.caption, color = c.ink, modifier = Modifier.padding(top = 6.dp)) }
    if (details.tags.isNotEmpty()) {
        FlowRow(
            Modifier.padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            details.tags.forEach { TagPill(it) }
        }
    }
}

/**
 * Small recessed tag.
 *
 * @param text Tag.
 */
@Composable
fun TagPill(text: String) {
    Text(
        "#$text",
        style = Vault.type.monoTag,
        color = Vault.colors.inkMuted,
        modifier = Modifier.clip(VaultRadius.pill).background(Vault.colors.well).padding(horizontal = 9.dp, vertical = 4.dp),
    )
}

@Composable
private fun CopyFigures(
    cost: Double?,
    value: Double?,
    shipping: Double?,
    indicative: Boolean,
) {
    val c = Vault.colors
    Row(Modifier.padding(top = 14.dp)) {
        Figure(if (shipping != null) "Payé + port" else "Payé", formatEuroOrDash(cost), Modifier.weight(1f))
        Figure(
            if (indicative) "Cote US" else "Cote",
            (
                if (indicative &&
                    value != null
                ) {
                    "≈ "
                } else {
                    ""
                }
            ) + formatEuroOrDash(value),
            Modifier.weight(1f),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Plus-value", style = Vault.type.caption, color = c.inkMuted)
            val gain = if (cost != null && value != null) value - cost else null
            when {
                gain == null -> Text("—", style = Vault.type.monoPrice, color = c.inkMuted)
                gain == 0.0 -> Text(formatEuro(0.0), style = Vault.type.monoPrice, color = c.ink)
                else -> PriceDelta(formatEuro(abs(gain)), gain > 0)
            }
        }
    }
}

@Composable
private fun Figure(
    label: String,
    value: String,
    modifier: Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = Vault.type.caption, color = Vault.colors.inkMuted)
        Text(value, style = Vault.type.monoPrice, color = Vault.colors.ink)
    }
}

@Composable
private fun Part(part: CopyPart) {
    val color = if (part.present) Vault.colors.indigoInk else Vault.colors.inkMuted
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(
            if (part.present) Icons.Default.Check else Icons.Default.Close,
            contentDescription = if (part.present) "Présent" else "Absent",
            tint = color,
            modifier = Modifier.size(14.dp),
        )
        Text(part.name, style = Vault.type.label, color = color)
        part.grade?.takeIf { part.present }?.let { Text(it.label, style = Vault.type.caption, color = Vault.colors.inkMuted) }
    }
}
