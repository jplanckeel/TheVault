package com.thevault.ui.screen.settings

import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.thevault.ui.component.ButtonVariant
import com.thevault.ui.component.VaultButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.time.LocalDate

/** Android: writes the inventory as PDF or CSV where the user chooses. */
@Composable
actual fun InsuranceExportSection(
    lines: suspend () -> List<InsuranceLine>,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun writer(write: (List<InsuranceLine>, OutputStream) -> Unit) =
        { uri: android.net.Uri? ->
            if (uri != null) {
                scope.launch {
                    val content = lines()
                    withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri)?.use { write(content, it) } }
                }
            }
        }
    val pdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf"), writer(::writePdf))
    val csv =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("text/csv"),
            writer {
                l,
                out,
                ->
                out.write(insuranceCsv(l).toByteArray())
            },
        )
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        VaultButton("PDF", {
            pdf.launch("thevault_inventaire.pdf")
        }, Modifier.weight(1f), ButtonVariant.Secondary, icon = Icons.Default.PictureAsPdf)
        VaultButton("CSV", {
            csv.launch("thevault_inventaire.csv")
        }, Modifier.weight(1f), ButtonVariant.Secondary, icon = Icons.Default.Description)
    }
}

private const val PAGE_W = 595
private const val PAGE_H = 842
private const val MARGIN = 40f
private const val LINE = 16f

/** A4 PDF: header, totals, one line per item, page breaks. */
private fun writePdf(
    lines: List<InsuranceLine>,
    out: OutputStream,
) {
    val doc = PdfDocument()
    val text = Paint().apply { textSize = 9f }
    val bold = Paint(text).apply { typeface = Typeface.DEFAULT_BOLD }
    val title = Paint(bold).apply { textSize = 16f }
    var pageNo = 1
    var page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create())
    var y = MARGIN + 10f
    page.canvas.drawText("TheVault — inventaire de collection", MARGIN, y, title)
    y += LINE * 1.5f
    page.canvas.drawText(
        "Édité le ${LocalDate.now()} · ${lines.size} objets · payé ${plainAmount(
            lines.sumOf { it.cost ?: 0.0 },
        )} € · valeur ${plainAmount(lines.sumOf { it.value ?: 0.0 })} €",
        MARGIN,
        y,
        text,
    )
    y += LINE * 1.5f
    val cols = floatArrayOf(MARGIN, 250f, 340f, 430f, 500f)
    listOf("Titre", "Plateforme", "État", "Payé", "Valeur").forEachIndexed { i, h -> page.canvas.drawText(h, cols[i], y, bold) }
    y += LINE
    for (l in lines) {
        if (y > PAGE_H - MARGIN) {
            doc.finishPage(page)
            page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, ++pageNo).create())
            y = MARGIN + 10f
        }
        val cells = listOf(l.title.take(40), l.platform.take(16), l.state.take(16), plainAmount(l.cost), plainAmount(l.value))
        cells.forEachIndexed { i, c -> page.canvas.drawText(c, cols[i], y, text) }
        y += LINE
    }
    doc.finishPage(page)
    doc.writeTo(out)
    doc.close()
}
