package com.thevault.ui.screen.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.thevault.ui.component.formatGroup
import com.thevault.ui.component.formatLongDate
import com.thevault.ui.component.label
import com.thevault.ui.screen.collection.CollectionEntry
import com.thevault.ui.screen.collection.ValueSource
import com.thevault.ui.screen.collection.cost
import com.thevault.ui.screen.collection.owned
import kotlin.math.roundToLong

/**
 * One line of the insurance inventory.
 *
 * @property title Title.
 * @property platform Platform.
 * @property kind Category.
 * @property state Completeness and edition.
 * @property purchase Purchase date, empty when unknown.
 * @property cost Price + shipping, null when unknown.
 * @property value Value, null when unknown.
 * @property source Origin of the value.
 */
data class InsuranceLine(
    val title: String,
    val platform: String,
    val kind: String,
    val state: String,
    val purchase: String,
    val cost: Double?,
    val value: Double?,
    val source: String,
)

/** Inventory of the owned items, most valuable first. Pure function, unit-tested. */
fun insuranceLines(entries: List<CollectionEntry>): List<InsuranceLine> =
    entries.owned().sortedByDescending { it.value ?: -1.0 }.map { e ->
        InsuranceLine(
            title = e.title,
            platform = e.platform,
            kind = e.kind.singular,
            state =
                listOfNotNull(
                    e.condition.formatGroup.label,
                    e.details.edition
                        .takeIf { it.name != "STANDARD" }
                        ?.label,
                    e.details.regionVariant,
                ).joinToString(" · "),
            purchase = e.acquiredAt?.let { formatLongDate(it) }.orEmpty(),
            cost = e.cost,
            value = e.value,
            source =
                when (e.valueSource) {
                    ValueSource.Ebay -> "Cote eBay PAL"
                    ValueSource.Manual -> "Valeur saisie"
                    ValueSource.UsIndicative -> "Cote US indicative"
                    ValueSource.None -> ""
                },
        )
    }

/** Amount with a decimal comma, for the exports (never masked). */
fun plainAmount(value: Double?): String {
    if (value == null) return ""
    val cents = (value * CENTS).roundToLong()
    return "${cents / CENTS.toLong()},${(cents % CENTS.toLong()).toString().padStart(2, '0')}"
}

/** Semicolon-separated CSV of the inventory (opens in French spreadsheets). */
fun insuranceCsv(lines: List<InsuranceLine>): String =
    buildString {
        appendLine("Titre;Plateforme;Type;État;Achat;Payé + port (€);Valeur (€);Source de la valeur")
        lines.forEach { l ->
            appendLine(
                listOf(
                    l.title,
                    l.platform,
                    l.kind,
                    l.state,
                    l.purchase,
                    plainAmount(l.cost),
                    plainAmount(l.value),
                    l.source,
                ).joinToString(";") {
                    cell(it)
                },
            )
        }
        appendLine(";;;;Total;${plainAmount(lines.sumOf { it.cost ?: 0.0 })};${plainAmount(lines.sumOf { it.value ?: 0.0 })};")
    }

private fun cell(text: String): String =
    if (text.any {
            it == ';' || it == '"' || it == '\n'
        }
    ) {
        "\"" + text.replace("\"", "\"\"") + "\""
    } else {
        text
    }

private const val CENTS = 100.0

/**
 * Platform buttons exporting the insurance inventory as PDF and CSV.
 *
 * @param lines Produces the inventory.
 * @param modifier Modifier.
 */
@Composable
expect fun InsuranceExportSection(
    lines: suspend () -> List<InsuranceLine>,
    modifier: Modifier = Modifier,
)
