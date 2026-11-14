package com.thevault

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import com.thevault.ui.app.AppViewModel
import com.thevault.ui.component.AchievementToast
import com.thevault.ui.component.clickableNoRipple
import com.thevault.ui.navigation.VaultNavHost
import com.thevault.ui.theme.ThemeMode
import com.thevault.ui.theme.Vault
import com.thevault.ui.theme.VaultTheme
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

/**
 * Root composable: theme, navigation and the achievement toast.
 *
 * @param onDarkShell Called with the shell darkness, to tint the system bars.
 */
@Composable
fun App(onDarkShell: (Boolean) -> Unit = {}) {
    setSingletonImageLoaderFactory { context ->
        ImageLoader
            .Builder(context)
            .components { add(KtorNetworkFetcherFactory()) }
            .crossfade(true)
            .build()
    }
    val app: AppViewModel = koinViewModel()
    val mode by app.themeMode.collectAsState()
    val dark = mode == ThemeMode.Black || mode == ThemeMode.System && isSystemInDarkTheme()
    LaunchedEffect(dark) { onDarkShell(dark) }
    VaultTheme(mode) {
        Box(Modifier.fillMaxSize().background(Vault.colors.shell)) {
            Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) { VaultNavHost(app) }
            AchievementOverlay(app, Modifier.align(Alignment.TopCenter))
        }
    }
}

@Composable
private fun AchievementOverlay(
    app: AppViewModel,
    modifier: Modifier,
) {
    val toast by app.toast.collectAsState()
    LaunchedEffect(toast?.id) {
        val id = toast?.id ?: return@LaunchedEffect
        delay(TOAST_MS)
        app.dismissToast(id)
    }
    AnimatedVisibility(
        visible = toast != null,
        modifier = modifier.windowInsetsPadding(WindowInsets.statusBars).padding(top = 8.dp, start = 16.dp, end = 16.dp),
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
    ) {
        toast?.let { def -> AchievementToast(def.title, def.xp, Modifier.clickableNoRipple { app.dismissToast(def.id) }) }
    }
}

private const val TOAST_MS = 4_000L
