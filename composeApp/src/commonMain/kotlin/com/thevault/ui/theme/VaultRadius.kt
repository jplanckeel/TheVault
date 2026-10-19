package com.thevault.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** Radius scale of the « Nuit profonde » design. */
object VaultRadius {
    /** 6dp — platform and format badges. */
    val badge = RoundedCornerShape(6.dp)

    /** 10dp — covers, segments. */
    val md = RoundedCornerShape(10.dp)

    /** 12dp — nested blocks, small buttons, inputs. */
    val lg = RoundedCornerShape(12.dp)

    /** 14dp — main buttons (52dp high), icon buttons, list rows. */
    val button = RoundedCornerShape(14.dp)

    /** 16dp — tiles, the add tab button. */
    val xl = RoundedCornerShape(16.dp)

    /** 18dp — cards. */
    val card = RoundedCornerShape(18.dp)

    /** 20dp — hero card. */
    val xxl = RoundedCornerShape(20.dp)

    /** Pill — filter chips. */
    val full = RoundedCornerShape(50)
}
