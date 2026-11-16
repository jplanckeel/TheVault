package com.thevault.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.thevault.ui.theme.Vault

/**
 * Pull to refresh container: pulling the content down calls [onRefresh]; the indicator
 * (indigo on a panel) spins while [isRefreshing].
 *
 * @param isRefreshing Whether a refresh is running.
 * @param onRefresh Starts a refresh.
 * @param modifier Modifier.
 * @param content Scrollable content.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultPullRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val state = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
        state = state,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = state,
                isRefreshing = isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter),
                containerColor = Vault.colors.panel,
                color = Vault.colors.indigo,
            )
        },
        content = content,
    )
}

/**
 * Outcome of a refresh, as a status LED line (green when done, red when offline).
 *
 * @param text Message.
 * @param success False when the server could not be reached.
 * @param modifier Modifier.
 */
@Composable
fun RefreshFeedback(
    text: String,
    success: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxWidth()) {
        StatusLed(if (success) LedColor.Sync else LedColor.Power, label = text)
    }
}
