package com.guigo.mocohub.data

import android.content.Context
import com.guigo.mocohub.AppLinks
import com.guigo.mocohub.model.PlayerStats
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.round

object PlayerStatsRepository {
    fun fetch(context: Context, tag: String): Pair<PlayerStats?, Boolean> {
        val clean = tag.trim().removePrefix("#").uppercase()
        return runCatching {
            val doc = Jsoup.connect(AppLinks.CELLSTRING_PLAYER_BASE + clean)
                .userAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/126 Safari/537.36 mo.co-hub")
                .referrer(AppLinks.CELLSTRING_HOME)
                .timeout(18_000)
                .get()
            val text = doc.body().text().replace(Regex("\\s+"), " ").trim()
            if (text.contains("Could not load profile", true) || text.contains("Failed to fetch", true)) {
                error("Perfil remoto indisponível")
            }

            fun findValue(vararg labels: String): String {
                for (label in labels) {
                    val e = Regex.escape(label)
                    Regex("(?i)$e\\s*[:#-]?\\s*([0-9][0-9.,kKmM]*)")
                        .find(text)?.groupValues?.getOrNull(1)?.let { return it }
                }
                return "—"
            }

            fun findStructuredValue(vararg keys: String): String {
                val html = doc.html()
                for (key in keys) {
                    val escaped = Regex.escape(key)
                    val patterns = listOf(
                        Regex("(?i)\"$escaped\"\\s*:\\s*\"?(\\d{1,6})\"?"),
                        Regex("(?i)$escaped\\s*[=:]\\s*\"?(\\d{1,6})\"?")
                    )
                    for (pattern in patterns) pattern.find(html)?.groupValues?.getOrNull(1)?.let { return it }
                }
                return "—"
            }

            fun findPercent(vararg labels: String): Int? {
                for (label in labels) {
                    val e = Regex.escape(label)
                    Regex("(?i)$e\\s*[:#-]?\\s*(\\d{1,3})(?:\\s*/\\s*100|%)?")
                        .find(text)?.groupValues?.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 100)?.let { return it }
                }
                return null
            }

            fun cleanName(raw: String): String = raw
                .replace(Regex("(?i)mo\\.co.*"), "")
                .replace(Regex("(?i)player profile.*"), "")
                .replace("| CellString", "", true)
                .trim(' ', '-', '|')

            val candidates = buildList {
                doc.selectFirst("meta[property=og:title]")?.attr("content")?.let(::add)
                doc.select("h1, h2, [class*=player-name], [class*=profile-name], [class*=name]").forEach { add(it.text()) }
            }.map(::cleanName)
                .filter { it.isNotBlank() && !it.equals(clean, true) && !it.contains("CellString", true) }
            val name = candidates.firstOrNull() ?: clean
            val title = doc.select("[class*=title], [class*=badge]").map { it.text().trim() }
                .firstOrNull { it.length in 2..32 && !it.contains("level", true) && !it.contains("rank", true) }.orEmpty()

            val eliteScore = findPercent("Elite")
            val grindScore = findPercent("Grind")
            val progressionScore = findPercent("Progression")
            val collectionScore = findPercent("Collection")
            val calculatedRating = if (listOf(eliteScore, grindScore, progressionScore, collectionScore).all { it != null }) {
                round(eliteScore!! * 0.30 + grindScore!! * 0.30 + progressionScore!! * 0.20 + collectionScore!! * 0.20).toInt().toString()
            } else "—"


            fun validNumber(value: String): String? = value
                .trim()
                .takeIf { it.matches(Regex("\\d{1,6}")) }

            fun findCurrentLevel(): String {
                // First use the exact textual strategy that worked in previous releases.
                validNumber(findValue("Season Level", "Season Lvl", "Current Level", "Current Lvl", "Hunter Level"))?.let { return it }

                // Only specific structured keys are safe. Generic `level` can mean career,
                // collector or another unrelated level.
                validNumber(findStructuredValue(
                    "currentLevel", "current_level", "seasonLevel", "season_level",
                    "seasonCurrentLevel", "season_current_level", "currentSeasonLevel"
                ))?.let { return it }

                // CellString can render the current level as a badge near the hunter name.
                val selectors = listOf(
                    "[class*=current-level]", "[class*=currentLevel]", "[class*=season-level]",
                    "[class*=seasonLevel]", "[class*=hunter-level]", "[class*=level-badge]",
                    "[class*=levelBadge]", "[data-current-level]", "[data-season-level]"
                )
                for (selector in selectors) {
                    for (element in doc.select(selector)) {
                        val candidates = listOf(
                            element.text(), element.attr("data-current-level"), element.attr("data-season-level"),
                            element.attr("data-level"), element.attr("aria-label"), element.attr("title")
                        )
                        for (candidate in candidates) {
                            Regex("(?<!\\d)(\\d{1,3})(?!\\d)").find(candidate)
                                ?.groupValues?.getOrNull(1)?.let { return it }
                        }
                    }
                }

                // Safe header heuristic: the level badge is commonly inside the same compact
                // header as the hunter name. Ignore the four rating component values.
                val scoreValues = listOfNotNull(eliteScore, grindScore, progressionScore, collectionScore).toSet()
                for (node in doc.select("h1, [class*=player-name], [class*=profile-name], [class*=profile-header], [class*=hunter]")) {
                    var parent: Element? = node
                    repeat(3) {
                        val local = parent?.text().orEmpty().replace(Regex("\\s+"), " ")
                        Regex("(?<![\\d.,])(\\d{1,3})(?![\\d.,])").findAll(local)
                            .mapNotNull { it.groupValues.getOrNull(1)?.toIntOrNull() }
                            .firstOrNull { it in 1..999 && it !in scoreValues }
                            ?.let { return it.toString() }
                        parent = parent?.parent()
                    }
                }
                return "—"
            }

            val stats = PlayerStats(
                name = name,
                tag = clean,
                careerLevel = findValue("Career Level", "Career Lvl", "Career"),
                currentLevel = findCurrentLevel(),
                collectorLevel = findValue("Collector Level", "Collector Lvl", "Collector"),
                eliteRank = findValue("Elite Rank", "Elite Leaderboard", "Leaderboard Rank"),
                eliteContracts = findValue("Elite Contracts", "Contracts"),
                rating = calculatedRating,
                eliteScore = eliteScore,
                grindScore = grindScore,
                progressionScore = progressionScore,
                collectionScore = collectionScore,
                title = title,
                eliteMerits = findValue("Elite Merits", "Merits"),
                careerTopPercent = findTopPercent(doc, "Career Level", "Career Lvl", "Career"),
                collectorTopPercent = findTopPercent(doc, "Collector Level", "Collector Lvl", "Collector"),
                eliteMeritsTopPercent = findTopPercent(doc, "Elite Merits", "Merits"),
                lastUpdated = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date())
            )
            // Preserve every previously valid public field when a temporary response omits it.
            val previous = load(context, clean)
            fun keep(current: String, old: String?): String = current.takeUnless { it.isBlank() || it == "—" }
                ?: old?.takeUnless { it.isBlank() || it == "—" }
                ?: "—"
            val mergedElite = stats.eliteScore ?: previous?.eliteScore
            val mergedGrind = stats.grindScore ?: previous?.grindScore
            val mergedProgression = stats.progressionScore ?: previous?.progressionScore
            val mergedCollection = stats.collectionScore ?: previous?.collectionScore
            val mergedRating = if (listOf(mergedElite, mergedGrind, mergedProgression, mergedCollection).all { it != null }) {
                round(mergedElite!! * 0.30 + mergedGrind!! * 0.30 + mergedProgression!! * 0.20 + mergedCollection!! * 0.20).toInt().toString()
            } else keep(stats.rating, previous?.rating)
            val merged = stats.copy(
                name = stats.name.takeUnless { it.isBlank() || it.equals(clean, true) } ?: previous?.name ?: clean,
                currentLevel = keep(stats.currentLevel, previous?.currentLevel),
                careerLevel = keep(stats.careerLevel, previous?.careerLevel),
                collectorLevel = keep(stats.collectorLevel, previous?.collectorLevel),
                eliteMerits = keep(stats.eliteMerits, previous?.eliteMerits),
                rating = mergedRating,
                eliteScore = mergedElite,
                grindScore = mergedGrind,
                progressionScore = mergedProgression,
                collectionScore = mergedCollection,
                title = stats.title.ifBlank { previous?.title.orEmpty() },
                careerTopPercent = stats.careerTopPercent ?: previous?.careerTopPercent,
                collectorTopPercent = stats.collectorTopPercent ?: previous?.collectorTopPercent,
                eliteMeritsTopPercent = stats.eliteMeritsTopPercent ?: previous?.eliteMeritsTopPercent
            )
            save(context, merged)
            merged to false
        }.getOrElse { load(context, clean)?.let { it to true } ?: (null to true) }
    }

    private fun findTopPercent(doc: Document, vararg labels: String): Int? {
        val topRegex = Regex("(?i)Top\\s*(\\d{1,3})\\s*%")
        for (label in labels) {
            val candidates = doc.getElementsContainingOwnText(label)
            for (element in candidates) {
                var node: Element? = element
                repeat(4) {
                    val local = node?.text().orEmpty()
                    topRegex.find(local)?.groupValues?.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 100)?.let { return it }
                    node = node?.parent()
                }
            }
        }
        return null
    }

    private fun save(context: Context, s: PlayerStats) {
        val j = JSONObject()
            .put("name", s.name).put("tag", s.tag).put("career", s.careerLevel).put("current", s.currentLevel)
            .put("collector", s.collectorLevel).put("rank", s.eliteRank).put("contracts", s.eliteContracts)
            .put("rating", s.rating).put("eliteScore", s.eliteScore).put("grindScore", s.grindScore)
            .put("progressionScore", s.progressionScore).put("collectionScore", s.collectionScore)
            .put("title", s.title).put("merits", s.eliteMerits)
            .put("careerTop", s.careerTopPercent ?: JSONObject.NULL)
            .put("collectorTop", s.collectorTopPercent ?: JSONObject.NULL)
            .put("meritsTop", s.eliteMeritsTopPercent ?: JSONObject.NULL)
            .put("updated", s.lastUpdated)
        context.getSharedPreferences("player_stats_cache", Context.MODE_PRIVATE).edit().putString(s.tag, j.toString()).apply()
    }

    private fun load(context: Context, tag: String): PlayerStats? {
        val raw = context.getSharedPreferences("player_stats_cache", Context.MODE_PRIVATE).getString(tag, null) ?: return null
        return runCatching {
            val j = JSONObject(raw)
            val elite = j.optInt("eliteScore", -1).takeIf { it >= 0 }
            val grind = j.optInt("grindScore", -1).takeIf { it >= 0 }
            val progression = j.optInt("progressionScore", -1).takeIf { it >= 0 }
            val collection = j.optInt("collectionScore", -1).takeIf { it >= 0 }
            val calculatedRating = if (listOf(elite, grind, progression, collection).all { it != null }) {
                round(elite!! * 0.30 + grind!! * 0.30 + progression!! * 0.20 + collection!! * 0.20).toInt().toString()
            } else "—"
            PlayerStats(
                name = j.optString("name", tag), tag = tag, careerLevel = j.optString("career", "—"), currentLevel = j.optString("current", "—"),
                collectorLevel = j.optString("collector", "—"), eliteRank = j.optString("rank", "—"), eliteContracts = j.optString("contracts", "—"),
                rating = calculatedRating, eliteScore = elite, grindScore = grind,
                progressionScore = progression, collectionScore = collection,
                title = j.optString("title", ""), eliteMerits = j.optString("merits", "—"),
                careerTopPercent = if (j.isNull("careerTop")) null else j.optInt("careerTop").coerceIn(0, 100),
                collectorTopPercent = if (j.isNull("collectorTop")) null else j.optInt("collectorTop").coerceIn(0, 100),
                eliteMeritsTopPercent = if (j.isNull("meritsTop")) null else j.optInt("meritsTop").coerceIn(0, 100),
                lastUpdated = j.optString("updated", "")
            )
        }.getOrNull()
    }
}
