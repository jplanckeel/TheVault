package com.thevault.ui.component

import com.thevault.domain.model.Edition
import com.thevault.domain.model.Grade
import com.thevault.domain.model.ItemStatus
import com.thevault.domain.model.PlayStatus
import com.thevault.domain.model.Region

/** French label of an ownership status. */
val ItemStatus.label: String
    get() =
        when (this) {
            ItemStatus.OWNED -> "Possédé"
            ItemStatus.WISHLIST -> "Envie"
            ItemStatus.SOLD -> "Vendu"
            ItemStatus.LOANED -> "Prêté"
        }

/** French label of a wear grade. */
val Grade.label: String
    get() =
        when (this) {
            Grade.MINT -> "Comme neuf"
            Grade.VERY_GOOD -> "Très bon"
            Grade.GOOD -> "Bon"
            Grade.FAIR -> "Correct"
            Grade.POOR -> "Abîmé"
        }

/** French label of an edition. */
val Edition.label: String
    get() =
        when (this) {
            Edition.STANDARD -> "Standard"
            Edition.BUDGET -> "Player's Choice / Selects"
            Edition.PLATINUM -> "Platinum"
            Edition.COLLECTOR -> "Collector"
            Edition.LIMITED -> "Limitée"
            Edition.BUNDLE -> "Pack"
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

/** Whether choosing this status calls for details (sale price, borrower…). */
val ItemStatus.needsDetails: Boolean get() = this == ItemStatus.SOLD || this == ItemStatus.LOANED

/** French label of a backlog status. */
val PlayStatus.label: String
    get() =
        when (this) {
            PlayStatus.TO_PLAY -> "À jouer"
            PlayStatus.PLAYING -> "En cours"
            PlayStatus.FINISHED -> "Fini"
            PlayStatus.COMPLETED -> "100 %"
            PlayStatus.DROPPED -> "Abandonné"
        }
