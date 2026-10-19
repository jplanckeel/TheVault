package com.thevault.ui.screen.gamedetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thevault.ui.component.BrandIcons
import com.thevault.ui.component.CardTitle
import com.thevault.ui.theme.OnSurfaceVariant
import com.thevault.ui.theme.SurfaceContainerLow
import com.thevault.ui.theme.VaultRadius

/**
 * Displays external marketplace and video links for a collectible item.
 *
 * @param query The main search query used for eBay, Amazon, and Fnac.
 * @param youtubeQuery The query used specifically for YouTube searches.
 */
@Composable
fun ExternalLinksSection(
    query: String,
    youtubeQuery: String = "$query trailer gameplay",
) {
    val uriHandler = LocalUriHandler.current

    val encodedQuery = query.replace(" ", "+")
    val encodedYt = youtubeQuery.replace(" ", "+")

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CardTitle("Ressources & marketplaces")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ExternalLinkButton(
                label = "Amazon",
                onClick = {
                    uriHandler.openUri("https://www.amazon.fr/s?k=$encodedQuery")
                },
                modifier = Modifier.weight(1f),
            )
            ExternalLinkButton(
                label = "eBay",
                onClick = {
                    uriHandler.openUri("https://www.ebay.fr/sch/i.html?_nkw=$encodedQuery")
                },
                modifier = Modifier.weight(1f),
            )
            ExternalLinkButton(
                label = "Fnac",
                onClick = {
                    uriHandler.openUri("https://www.fnac.com/SearchResult/ResultList.aspx?Search=$encodedQuery")
                },
                modifier = Modifier.weight(1f),
            )
            ExternalLinkButton(
                label = "YouTube",
                onClick = {
                    uriHandler.openUri("https://www.youtube.com/results?search_query=$encodedYt")
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ExternalLinkButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .clip(VaultRadius.xl)
                .background(SurfaceContainerLow)
                .clickable { onClick() }
                .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val iconVector =
            when (label) {
                "Amazon" -> BrandIcons.Amazon(brandColor = OnSurfaceVariant, accentColor = OnSurfaceVariant)
                "eBay" -> BrandIcons.Ebay(brandColor = OnSurfaceVariant, accentColor = OnSurfaceVariant)
                "YouTube" -> BrandIcons.YouTube(brandColor = OnSurfaceVariant, accentColor = OnSurfaceVariant)
                "Fnac" -> Icons.Default.ShoppingBag
                else -> Icons.Default.OpenInNew
            }

        Icon(
            imageVector = iconVector,
            contentDescription = label,
            tint = OnSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = OnSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
