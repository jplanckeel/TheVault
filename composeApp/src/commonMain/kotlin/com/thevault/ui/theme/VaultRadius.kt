package com.thevault.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** Radius scale of the console design. */
object VaultRadius {
    /** 6dp — platform and condition tags. */
    val xs = RoundedCornerShape(6.dp)

    /** 10dp — small surfaces. */
    val sm = RoundedCornerShape(10.dp)

    /** 11dp — segments inside a segmented control. */
    val segment = RoundedCornerShape(11.dp)

    /** 12dp — small icon buttons. */
    val iconSm = RoundedCornerShape(12.dp)

    /** 14dp — icon buttons, search, segmented controls, chart screen. */
    val md = RoundedCornerShape(14.dp)

    /** 16dp — level badge. */
    val badge = RoundedCornerShape(16.dp)

    /** 22dp — panels and cards. */
    val lg = RoundedCornerShape(22.dp)

    /** 30dp — tab bar. */
    val xl = RoundedCornerShape(30.dp)

    /** Top corners of a bottom sheet. */
    val sheet = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)

    /** Pill — buttons and chips. */
    val pill = RoundedCornerShape(percent = 50)
}
