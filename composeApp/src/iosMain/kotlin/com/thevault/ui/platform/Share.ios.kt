package com.thevault.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

/** iOS share sheet (UIActivityViewController) presented from the key window. */
@Composable
actual fun rememberShareText(): (String) -> Unit =
    remember {
        { text ->
            val controller = UIActivityViewController(activityItems = listOf(text), applicationActivities = null)
            UIApplication.sharedApplication.keyWindow?.rootViewController?.presentViewController(
                controller,
                animated = true,
                completion = null,
            )
        }
    }
