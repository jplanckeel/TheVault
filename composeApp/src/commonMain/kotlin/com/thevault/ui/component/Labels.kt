package com.thevault.ui.component

import com.thevault.domain.model.ItemCondition
import com.thevault.domain.model.ItemStatus
import com.thevault.domain.model.Region

/** French label of an ownership status. */
val ItemStatus.label: String
    get() =
        when (this) {
            ItemStatus.OWNED -> "Possédé"
            ItemStatus.WISHLIST -> "Wishlist"
            ItemStatus.SOLD -> "Vendu"
            ItemStatus.LOANED -> "Prêté"
        }

/** French label of a physical condition. */
val ItemCondition.label: String
    get() =
        when (this) {
            ItemCondition.MINT -> "Mint"
            ItemCondition.VERY_GOOD -> "Très bon"
            ItemCondition.GOOD -> "Bon"
            ItemCondition.FAIR -> "Correct"
            ItemCondition.POOR -> "Abîmé"
            ItemCondition.LOOSE -> "Loose"
            ItemCondition.CIB_NO_MANUAL -> "Complet sans notice"
            ItemCondition.CIB -> "Complet (CIB)"
            ItemCondition.NEW_SEALED -> "Neuf scellé"
        }

/** Display label of a region. */
val Region.label: String
    get() =
        when (this) {
            Region.PAL -> "PAL"
            Region.NTSC_US -> "NTSC-U"
            Region.NTSC_JP -> "NTSC-J"
            Region.OTHER -> "Autre"
        }
