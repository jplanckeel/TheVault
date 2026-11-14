package com.thevault.domain.model

/**
 * Static catalog of known IGDB platforms with their manufacturer and release year.
 * Used to enrich [PlatformEntry] when no matching [Console] exists in the local database.
 *
 * Source: https://www.igdb.com/platforms
 */
object IgdbPlatformCatalog {
    data class PlatformInfo(
        val manufacturer: String,
        val releaseYear: Int,
    )

    /**
     * Maps a platform name (case-insensitive) to its [PlatformInfo].
     * Keys are lowercase for matching.
     */
    private val catalog: Map<String, PlatformInfo> =
        mapOf(
            // ── Sony ──────────────────────────────────────────────────────────────
            "playstation 5" to PlatformInfo("Sony", 2020),
            "ps5" to PlatformInfo("Sony", 2020),
            "playstation 4" to PlatformInfo("Sony", 2013),
            "playstation 4 pro" to PlatformInfo("Sony", 2016),
            "ps4" to PlatformInfo("Sony", 2013),
            "playstation 3" to PlatformInfo("Sony", 2006),
            "ps3" to PlatformInfo("Sony", 2006),
            "playstation 2" to PlatformInfo("Sony", 2000),
            "ps2" to PlatformInfo("Sony", 2000),
            "playstation" to PlatformInfo("Sony", 1994),
            "ps1" to PlatformInfo("Sony", 1994),
            "psx" to PlatformInfo("Sony", 1994),
            "playstation portable" to PlatformInfo("Sony", 2004),
            "psp" to PlatformInfo("Sony", 2004),
            "playstation vita" to PlatformInfo("Sony", 2011),
            "ps vita" to PlatformInfo("Sony", 2011),
            // ── Microsoft ─────────────────────────────────────────────────────────
            "xbox series x" to PlatformInfo("Microsoft", 2020),
            "xbox series s" to PlatformInfo("Microsoft", 2020),
            "xbox series x/s" to PlatformInfo("Microsoft", 2020),
            "xbox one" to PlatformInfo("Microsoft", 2013),
            "xbox one x" to PlatformInfo("Microsoft", 2017),
            "xbox one s" to PlatformInfo("Microsoft", 2016),
            "xbox 360" to PlatformInfo("Microsoft", 2005),
            "xbox" to PlatformInfo("Microsoft", 2001),
            "pc" to PlatformInfo("Microsoft", 1981),
            "windows" to PlatformInfo("Microsoft", 1985),
            "pc (microsoft windows)" to PlatformInfo("Microsoft", 1985),
            // ── Nintendo ──────────────────────────────────────────────────────────
            "nintendo switch 2" to PlatformInfo("Nintendo", 2025),
            "switch 2" to PlatformInfo("Nintendo", 2025),
            "nintendo switch" to PlatformInfo("Nintendo", 2017),
            "switch" to PlatformInfo("Nintendo", 2017),
            "wii u" to PlatformInfo("Nintendo", 2012),
            "nintendo wii u" to PlatformInfo("Nintendo", 2012),
            "wii" to PlatformInfo("Nintendo", 2006),
            "nintendo wii" to PlatformInfo("Nintendo", 2006),
            "nintendo gamecube" to PlatformInfo("Nintendo", 2001),
            "gamecube" to PlatformInfo("Nintendo", 2001),
            "ngc" to PlatformInfo("Nintendo", 2001),
            "nintendo 64" to PlatformInfo("Nintendo", 1996),
            "n64" to PlatformInfo("Nintendo", 1996),
            "super nintendo entertainment system" to PlatformInfo("Nintendo", 1990),
            "super nintendo" to PlatformInfo("Nintendo", 1990),
            "snes" to PlatformInfo("Nintendo", 1990),
            "super famicom" to PlatformInfo("Nintendo", 1990),
            "nintendo entertainment system" to PlatformInfo("Nintendo", 1983),
            "nes" to PlatformInfo("Nintendo", 1983),
            "famicom" to PlatformInfo("Nintendo", 1983),
            "nintendo 3ds" to PlatformInfo("Nintendo", 2011),
            "new nintendo 3ds" to PlatformInfo("Nintendo", 2014),
            "3ds" to PlatformInfo("Nintendo", 2011),
            "nintendo ds" to PlatformInfo("Nintendo", 2004),
            "nds" to PlatformInfo("Nintendo", 2004),
            "nintendo dsi" to PlatformInfo("Nintendo", 2008),
            "game boy advance sp" to PlatformInfo("Nintendo", 2003),
            "game boy advance" to PlatformInfo("Nintendo", 2001),
            "gba" to PlatformInfo("Nintendo", 2001),
            "game boy color" to PlatformInfo("Nintendo", 1998),
            "gbc" to PlatformInfo("Nintendo", 1998),
            "game boy" to PlatformInfo("Nintendo", 1989),
            "gb" to PlatformInfo("Nintendo", 1989),
            "virtual boy" to PlatformInfo("Nintendo", 1995),
            // ── Sega ──────────────────────────────────────────────────────────────
            "sega dreamcast" to PlatformInfo("Sega", 1998),
            "dreamcast" to PlatformInfo("Sega", 1998),
            "sega saturn" to PlatformInfo("Sega", 1994),
            "saturn" to PlatformInfo("Sega", 1994),
            "sega mega drive" to PlatformInfo("Sega", 1988),
            "mega drive" to PlatformInfo("Sega", 1988),
            "sega genesis" to PlatformInfo("Sega", 1988),
            "genesis" to PlatformInfo("Sega", 1988),
            "sega master system" to PlatformInfo("Sega", 1985),
            "master system" to PlatformInfo("Sega", 1985),
            "sega game gear" to PlatformInfo("Sega", 1990),
            "game gear" to PlatformInfo("Sega", 1990),
            "sega 32x" to PlatformInfo("Sega", 1994),
            "sega cd" to PlatformInfo("Sega", 1991),
            "mega-cd" to PlatformInfo("Sega", 1991),
            // ── Atari ─────────────────────────────────────────────────────────────
            "atari 2600" to PlatformInfo("Atari", 1977),
            "atari 5200" to PlatformInfo("Atari", 1982),
            "atari 7800" to PlatformInfo("Atari", 1986),
            "atari jaguar" to PlatformInfo("Atari", 1993),
            "atari lynx" to PlatformInfo("Atari", 1989),
            "atari st" to PlatformInfo("Atari", 1985),
            // ── SNK ───────────────────────────────────────────────────────────────
            "neo geo" to PlatformInfo("SNK", 1990),
            "neo geo pocket" to PlatformInfo("SNK", 1998),
            "neo geo pocket color" to PlatformInfo("SNK", 1999),
            // ── NEC ───────────────────────────────────────────────────────────────
            "pc engine" to PlatformInfo("NEC", 1987),
            "turbografx-16" to PlatformInfo("NEC", 1987),
            "pc-fx" to PlatformInfo("NEC", 1994),
            // ── 3DO ───────────────────────────────────────────────────────────────
            "3do" to PlatformInfo("3DO", 1993),
            "3do interactive multiplayer" to PlatformInfo("3DO", 1993),
        )

    /**
     * Returns the [PlatformInfo] for a given platform name, or null if unknown.
     *
     * Matching strategy:
     * 1. Exact match (case-insensitive)
     * 2. Keyword-based fallback for known manufacturers
     */
    fun find(platformName: String): PlatformInfo? {
        val key = platformName.lowercase().trim()

        // 1. Exact match
        catalog[key]?.let { return it }

        // 2. Keyword fallback — covers IGDB names like "Nintendo GameCube", "Nintendo Switch 2", etc.
        return when {
            key.contains("nintendo") || key.contains("game boy") || key.contains("gameboy") ->
                inferNintendoYear(key)
            key.contains("playstation") || key.startsWith("ps") ->
                inferSonyYear(key)
            key.contains("xbox") ->
                inferMicrosoftYear(key)
            key.contains("sega") || key.contains("mega drive") || key.contains("dreamcast") || key.contains("saturn") ->
                inferSegaYear(key)
            key.contains("atari") ->
                PlatformInfo("Atari", 1977)
            key.contains("neo geo") ->
                PlatformInfo("SNK", 1990)
            else -> null
        }
    }

    private fun inferNintendoYear(key: String): PlatformInfo {
        val year =
            when {
                key.contains("switch 2") -> 2025
                key.contains("switch") -> 2017
                key.contains("wii u") -> 2012
                key.contains("wii") -> 2006
                key.contains("gamecube") || key.contains("game cube") -> 2001
                key.contains("64") -> 1996
                key.contains("super") && (key.contains("nintendo") || key.contains("famicom") || key.contains("snes")) -> 1990
                key.contains("famicom") || key.contains("nes") -> 1983
                key.contains("3ds") -> 2011
                key.contains("ds") -> 2004
                key.contains("game boy advance") || key.contains("gba") -> 2001
                key.contains("game boy color") || key.contains("gbc") -> 1998
                key.contains("game boy") || key.contains("gameboy") -> 1989
                key.contains("virtual boy") -> 1995
                else -> 0
            }
        return PlatformInfo("Nintendo", year)
    }

    private fun inferSonyYear(key: String): PlatformInfo {
        val year =
            when {
                key.contains("5") -> 2020
                key.contains("4") -> 2013
                key.contains("3") -> 2006
                key.contains("2") -> 2000
                key.contains("vita") -> 2011
                key.contains("portable") || key.contains("psp") -> 2004
                else -> 1994
            }
        return PlatformInfo("Sony", year)
    }

    private fun inferMicrosoftYear(key: String): PlatformInfo {
        val year =
            when {
                key.contains("series") -> 2020
                key.contains("one") -> 2013
                key.contains("360") -> 2005
                else -> 2001
            }
        return PlatformInfo("Microsoft", year)
    }

    private fun inferSegaYear(key: String): PlatformInfo {
        val year =
            when {
                key.contains("dreamcast") -> 1998
                key.contains("saturn") -> 1994
                key.contains("32x") -> 1994
                key.contains("cd") || key.contains("mega-cd") -> 1991
                key.contains("game gear") -> 1990
                key.contains("mega drive") || key.contains("genesis") -> 1988
                key.contains("master system") -> 1985
                else -> 1988
            }
        return PlatformInfo("Sega", year)
    }
}
