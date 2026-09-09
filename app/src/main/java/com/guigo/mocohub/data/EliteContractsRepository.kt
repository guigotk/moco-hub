package com.guigo.mocohub.data

import android.content.Context
import com.guigo.mocohub.AppLinks
import com.guigo.mocohub.model.EliteContractEntry
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.json.JSONArray
import org.json.JSONObject

object EliteContractsRepository {
    fun fetch(context: Context): Pair<List<EliteContractEntry>, Boolean> {
        return runCatching {
            val doc = Jsoup.connect(AppLinks.CELLSTRING_ELITE_CONTRACTS)
                .userAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/126 Safari/537.36 mo.co-hub")
                .referrer(AppLinks.CELLSTRING_HOME)
                .timeout(18_000)
                .get()

            val result = linkedMapOf<String, EliteContractEntry>()

            fun addCandidate(container: Element, playerName: String? = null) {
                val text = container.text().replace(Regex("\\s+"), " ").trim()
                if (text.isBlank()) return
                val name = playerName?.trim().orEmpty().ifBlank {
                    container.selectFirst("a[href*=/moco/player/], [class*=name], [class*=player]")?.text()?.trim().orEmpty()
                }
                if (name.isBlank() || name.length > 45) return
                val rank = Regex("(?:^|\\s)#?(\\d{1,4})(?:[.)]|\\s)").find(text)?.groupValues?.getOrNull(1)
                    ?: Regex("(?i)rank\\s*#?\\s*(\\d{1,4})").find(text)?.groupValues?.getOrNull(1)
                    ?: return
                val nums = Regex("(?<![A-Za-z])([0-9][0-9.,kKmM]*)(?![A-Za-z])").findAll(text).map { it.groupValues[1] }.toList()
                val value = nums.lastOrNull { it != rank } ?: return
                result.putIfAbsent("$rank|$name", EliteContractEntry(rank, name, value))
            }

            doc.select("table tbody tr, table tr").forEach { tr ->
                val cells = tr.select("td")
                if (cells.size >= 2) {
                    val rank = cells.first()?.text()?.trim()?.removePrefix("#").orEmpty()
                    val name = cells.getOrNull(1)?.text()?.trim().orEmpty()
                    val value = cells.last()?.text()?.trim().orEmpty()
                    if (rank.any(Char::isDigit) && name.isNotBlank() && value.isNotBlank()) {
                        result.putIfAbsent("$rank|$name", EliteContractEntry(rank, name, value))
                    }
                }
            }

            doc.select("a[href*=/moco/player/]").forEach { a ->
                var parent: Element? = a.parent()
                repeat(5) {
                    if (parent != null && parent!!.text().length in 4..220) addCandidate(parent!!, a.text())
                    parent = parent?.parent()
                }
            }

            if (result.isEmpty()) {
                doc.select("[class*=leaderboard] [class*=row], [class*=ranking] [class*=row], [class*=entry], [class*=player]")
                    .forEach { addCandidate(it) }
            }

            if (result.isEmpty()) {
                // Muitas páginas modernas carregam o leaderboard em JSON dentro de <script>.
                // Tentamos ler esse payload antes de cair para o texto visível.
                val scripts = doc.select("script").joinToString(" ") { it.data() + " " + it.html() }
                val patterns = listOf(
                    Regex(""""(?:rank|position)"\s*:\s*(\d{1,4}).{0,500}?"(?:name|playerName|username)"\s*:\s*"([^"]{2,45})".{0,500}?"(?:eliteContracts|contracts|score|value)"\s*:\s*"?([0-9][0-9.,kKmM]*)""", RegexOption.IGNORE_CASE),
                    Regex(""""(?:name|playerName|username)"\s*:\s*"([^"]{2,45})".{0,500}?"(?:eliteContracts|contracts|score|value)"\s*:\s*"?([0-9][0-9.,kKmM]*)""", RegexOption.IGNORE_CASE)
                )
                patterns[0].findAll(scripts).take(200).forEach { m ->
                    val r=m.groupValues[1]; val n=m.groupValues[2].trim(); val v=m.groupValues[3]
                    result.putIfAbsent("$r|$n", EliteContractEntry(r,n,v))
                }
                if (result.isEmpty()) {
                    patterns[1].findAll(scripts).take(200).forEachIndexed { i, m ->
                        val n=m.groupValues[1].trim(); val v=m.groupValues[2]; val r=(i+1).toString()
                        result.putIfAbsent("$r|$n", EliteContractEntry(r,n,v))
                    }
                }
            }

            if (result.isEmpty()) {
                val text = doc.body().text().replace(Regex("\\s+"), " ")
                Regex("(?:^|\\s)#?(\\d{1,4})\\s+([\\p{L}0-9_ .|'-]{2,40}?)\\s+([0-9][0-9.,kKmM]*)")
                    .findAll(text).take(200).forEach { m ->
                        val r=m.groupValues[1]; val n=m.groupValues[2].trim(); val v=m.groupValues[3]
                        result.putIfAbsent("$r|$n", EliteContractEntry(r,n,v))
                    }
            }

            val rows = result.values.sortedBy { it.rank.filter(Char::isDigit).toIntOrNull() ?: Int.MAX_VALUE }.take(200)
            if (rows.isEmpty()) error("Leaderboard sem dados interpretáveis")
            saveCache(context, rows)
            rows to false
        }.getOrElse { loadCache(context) to true }
    }

    private fun saveCache(context: Context, rows: List<EliteContractEntry>) {
        val a = JSONArray()
        rows.forEach { a.put(JSONObject().put("r", it.rank).put("n", it.name).put("v", it.value)) }
        context.getSharedPreferences("elite_contracts_cache", Context.MODE_PRIVATE).edit().putString("rows", a.toString()).apply()
    }

    private fun loadCache(context: Context): List<EliteContractEntry> {
        val raw = context.getSharedPreferences("elite_contracts_cache", Context.MODE_PRIVATE).getString("rows", null) ?: return emptyList()
        return runCatching {
            val a = JSONArray(raw)
            (0 until a.length()).map { i -> a.getJSONObject(i).let { EliteContractEntry(it.optString("r"), it.optString("n"), it.optString("v")) } }
        }.getOrDefault(emptyList())
    }
}
