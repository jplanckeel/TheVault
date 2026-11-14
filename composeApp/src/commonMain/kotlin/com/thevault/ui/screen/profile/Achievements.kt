@file:Suppress("MagicNumber") // Design tokens and geometry of the console mockups.

package com.thevault.ui.screen.profile

import com.thevault.ui.component.FormatGroup
import com.thevault.ui.component.Manufacturer
import com.thevault.ui.component.formatGroup
import com.thevault.ui.screen.collection.CollectionEntry
import com.thevault.ui.screen.collection.ItemKind
import com.thevault.ui.screen.collection.owned
import com.thevault.ui.screen.collection.totalValue
import kotlin.math.roundToLong

/**
 * Achievement computed from the real collection.
 *
 * @property id Stable identifier (stored when unlocked).
 * @property title Title.
 * @property description Goal.
 * @property xp XP granted.
 * @property target Goal value.
 * @property measure Current value from the owned items.
 */
data class AchievementDef(
    val id: String,
    val title: String,
    val description: String,
    val xp: Int,
    val target: Long,
    val measure: (List<CollectionEntry>) -> Long,
)

/**
 * Progress of one achievement.
 *
 * @property def Definition.
 * @property progress Current value.
 */
data class AchievementProgress(
    val def: AchievementDef,
    val progress: Long,
) {
    /** Whether the goal is reached. */
    val unlocked: Boolean get() = progress >= def.target
}

private fun List<CollectionEntry>.games() = filter { it.kind == ItemKind.Game }

private fun List<CollectionEntry>.bestPlatformCount(predicate: (CollectionEntry) -> Boolean): Long =
    games()
        .filter(predicate)
        .groupingBy { it.platform }
        .eachCount()
        .values
        .maxOrNull()
        ?.toLong() ?: 0L

private val sixteenBits = Regex("""super nintendo|snes|super famicom|mega ?drive|genesis""", RegexOption.IGNORE_CASE)

/** Every achievement, in display order. */
val Achievements: List<AchievementDef> =
    listOf(
        AchievementDef("first", "Premier dépôt", "Ranger un premier objet dans le coffre", 50, 1) { it.size.toLong() },
        AchievementDef("fifty", "Collectionneur", "50 objets dans le coffre", 100, 50) { it.size.toLong() },
        AchievementDef("archivist", "Archiviste", "250 objets dans le coffre", 250, 250) { it.size.toLong() },
        AchievementDef("legend", "Musée privé", "1 000 objets dans le coffre", 500, 1000) { it.size.toLong() },
        AchievementDef("bits16", "Écurie 16 bits", "40 jeux Super Nintendo ou Mega Drive", 150, 40) { list ->
            list.games().count { sixteenBits.containsMatchIn(it.platform) }.toLong()
        },
        AchievementDef("complete", "Tout complet", "20 jeux Complet sur une même plateforme", 150, 20) { list ->
            list.bestPlatformCount { it.condition.formatGroup == FormatGroup.Complete }
        },
        AchievementDef("sealed", "Sous blister", "5 objets neufs ou scellés", 100, 5) { list ->
            list.count { it.condition.formatGroup == FormatGroup.New || it.condition.formatGroup == FormatGroup.Sealed }.toLong()
        },
        AchievementDef("platforms", "Multi-plateformes", "Des jeux sur 6 plateformes", 100, 6) { list ->
            list
                .games()
                .map { it.platform }
                .filter { it.isNotBlank() }
                .distinct()
                .size
                .toLong()
        },
        AchievementDef("makers", "Trois constructeurs", "Des consoles de 3 constructeurs", 100, 3) { list ->
            list
                .filter { it.kind == ItemKind.Console && it.manufacturer != Manufacturer.Other }
                .map { it.manufacturer }
                .distinct()
                .size
                .toLong()
        },
        AchievementDef("gem", "Pièce maîtresse", "Un objet coté 200 € ou plus", 100, 200) { list ->
            (list.maxOfOrNull { it.value ?: 0.0 } ?: 0.0).roundToLong()
        },
        AchievementDef("value1k", "Coffre-fort", "Dépasser 1 000 € de valeur", 100, 1000) { it.totalValue().roundToLong() },
        AchievementDef("value10k", "Collection légendaire", "Dépasser 10 000 € de valeur", 300, 10_000) { it.totalValue().roundToLong() },
        AchievementDef("bookkeeper", "Comptable", "Prix d'achat renseigné sur 50 objets", 100, 50) { list ->
            list.count { it.purchasePrice != null }.toLong()
        },
    )

/** Progress of every achievement for the owned items of [all]. */
fun achievementProgress(all: List<CollectionEntry>): List<AchievementProgress> {
    val owned = all.owned()
    return Achievements.map { AchievementProgress(it, it.measure(owned)) }
}

/**
 * Player level.
 *
 * @property level Level number (from 1).
 * @property title Level title.
 * @property xpInLevel XP earned inside the level.
 * @property xpForNext XP needed to reach the next level.
 * @property totalXp Total XP.
 */
data class PlayerLevel(
    val level: Int,
    val title: String,
    val xpInLevel: Long,
    val xpForNext: Long,
    val totalXp: Long,
)

private const val XP_PER_ITEM = 5L
private const val LEVEL_STEP = 100L

/** Total XP: 5 per owned item plus the XP of the unlocked achievements. */
fun totalXp(
    ownedCount: Int,
    progress: List<AchievementProgress>,
): Long = ownedCount * XP_PER_ITEM + progress.filter { it.unlocked }.sumOf { it.def.xp.toLong() }

/** Level of [xp]: level n needs 100 × n XP more than level n − 1. */
fun levelOf(xp: Long): PlayerLevel {
    var level = 1
    var floor = 0L
    while (xp >= floor + LEVEL_STEP * level) {
        floor += LEVEL_STEP * level
        level++
    }
    return PlayerLevel(level, levelTitle(level), xp - floor, LEVEL_STEP * level, xp)
}

private fun levelTitle(level: Int): String =
    when {
        level < 3 -> "Débutant"
        level < 6 -> "Amateur"
        level < 10 -> "Collectionneur"
        level < 15 -> "Archiviste"
        else -> "Conservateur"
    }
