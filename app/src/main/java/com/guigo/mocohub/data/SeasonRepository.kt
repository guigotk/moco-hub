package com.guigo.mocohub.data

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/**
 * Calendário local da temporada. O marco abaixo corresponde ao início da Temporada 5.
 * As temporadas seguintes avançam mês a mês.
 */
object SeasonRepository {
    private val season5Start = Instant.parse("2026-09-10T08:00:00Z")

    data class SeasonInfo(
        val currentSeason: Int,
        val title: String,
        val subtitle: String,
        val progressPercent: Int
    )

    fun current(now: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()): SeasonInfo {
        val start5 = season5Start.atZone(zone)
        val nowZ = now.atZone(zone)
        if (nowZ.isBefore(start5)) {
            val remaining = formatRemaining(nowZ, start5)
            val previousStart = start5.minusMonths(1)
            val total = ChronoUnit.SECONDS.between(previousStart, start5).coerceAtLeast(1)
            val elapsed = ChronoUnit.SECONDS.between(previousStart, nowZ).coerceAtLeast(0)
            return SeasonInfo(4, "Temporada 4", "Temporada 5 começa em $remaining", ((elapsed * 100) / total).toInt().coerceIn(0, 100))
        }

        var season = 5
        var start = start5
        var end = start.plusMonths(1)
        while (!nowZ.isBefore(end)) {
            season++
            start = end
            end = start.plusMonths(1)
        }
        val total = ChronoUnit.SECONDS.between(start, end).coerceAtLeast(1)
        val elapsed = ChronoUnit.SECONDS.between(start, nowZ).coerceAtLeast(0)
        return SeasonInfo(season, "Temporada $season", "Termina em ${formatRemaining(nowZ, end)}", ((elapsed * 100) / total).toInt().coerceIn(0, 100))
    }

    private fun formatRemaining(from: ZonedDateTime, to: ZonedDateTime): String {
        val mins = ChronoUnit.MINUTES.between(from, to).coerceAtLeast(0)
        val days = mins / 1440
        val hours = (mins % 1440) / 60
        val minutes = mins % 60
        return when {
            days > 0 -> "${days}d ${hours}h"
            hours > 0 -> "${hours}h ${minutes}min"
            else -> "${minutes}min"
        }
    }
}
