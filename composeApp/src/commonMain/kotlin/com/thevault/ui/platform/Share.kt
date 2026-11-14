package com.thevault.ui.platform

import androidx.compose.runtime.Composable

/** Opens the system share sheet with a text (a share link…). */
@Composable
expect fun rememberShareText(): (String) -> Unit
