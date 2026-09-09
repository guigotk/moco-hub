package com.guigo.mocohub.data

import com.guigo.mocohub.AppLinks
import com.guigo.mocohub.model.NewsItem
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * Notícias do mo.co hub.
 *
 * A fonte principal é o CellString porque o HTML público do X é instável para apps sem API autenticada.
 * A Home recebe cards limpos: título, descrição, imagem, data e link da matéria.
 */
object NewsRepository {
    fun fetch(): List<NewsItem> {
        runCatching { fetchCellString() }.getOrNull()?.takeIf { it.isNotEmpty() }?.let { return it }
        return curatedFallback()
    }

    private fun fetchCellString(): List<NewsItem> {
        val hub = Jsoup.connect(AppLinks.CELLSTRING_NEWS)
            .userAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/126 Safari/537.36 mo.co-hub")
            .referrer(AppLinks.CELLSTRING_HOME)
            .timeout(15_000)
            .get()

        val urls = linkedSetOf<String>()
        hub.select("a[href*='/news/article/'], a[href*='/news/news/'], a[href*='/news/interview/']").forEach { a ->
            val href = a.absUrl("href").ifBlank { a.attr("href").let { if (it.startsWith("/")) "https://cellstring.com$it" else it } }
            val label = clean(a.text())
            if (href.isNotBlank() && (label.contains("mo.co", true) || label.contains("moco", true) || href.contains("moco", true))) urls += href
        }

        val parsed = urls.take(8).mapNotNull { url -> runCatching { parseArticle(url) }.getOrNull() }
        return (parsed + curatedFallback()).distinctBy { it.url }.take(8)
    }

    private fun parseArticle(url: String): NewsItem? {
        val doc = Jsoup.connect(url)
            .userAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/126 Safari/537.36 mo.co-hub")
            .timeout(15_000)
            .get()
        val title = meta(doc, "meta[property=og:title]", "content")
            .ifBlank { clean(doc.selectFirst("h1")?.text().orEmpty()) }
        if (title.isBlank()) return null
        val description = meta(doc, "meta[property=og:description]", "content")
            .ifBlank { clean(doc.selectFirst("article p, main p")?.text().orEmpty()).take(180) }
        val image = meta(doc, "meta[property=og:image]", "content")
        val published = meta(doc, "meta[property=article:published_time]", "content")
            .ifBlank { clean(doc.selectFirst("time")?.text().orEmpty()) }
        return NewsItem(title = title, summary = description.take(180), url = url, source = "CellString", published = published, imageUrl = image)
    }

    private fun meta(doc: Document, selector: String, attr: String) = doc.selectFirst(selector)?.attr(attr)?.trim().orEmpty()
    private fun clean(value: String) = value.replace(Regex("\\s+"), " ").trim()

    /**
     * Fallback editorial conhecido. Evita cards quebrados quando o hub do CellString não lista mo.co entre os posts mais recentes.
     */
    private fun curatedFallback() = listOf(
        NewsItem(
            title = "O que era mo.co antes do lançamento global?",
            summary = "Uma retrospectiva do desenvolvimento de mo.co, da fase de testes até o lançamento global.",
            url = "https://cellstring.com/news/article/what-was-moco-like-before-its-global-launch",
            source = "CellString",
            published = "13 jun 2026"
        ),
        NewsItem(
            title = "Cinco razões pelas quais mo.co pode continuar vivo por muitos anos",
            summary = "Uma análise comunitária sobre a evolução do jogo após a atualização neo mo.co.",
            url = "https://cellstring.com/news/article/five-reasons-why-moco-will-be-alive",
            source = "CellString",
            published = "6 jun 2026"
        ),
        NewsItem(
            title = "mo.co e Marshall Columbia: a primeira colaboração do jogo",
            summary = "Detalhes da colaboração que trouxe novos cosméticos e itens ao universo de mo.co.",
            url = "https://cellstring.com/news/news/moco-marshall-columbia-what-do-we-know-about-the-first-mocollaboration",
            source = "CellString",
            published = "3 jun 2026"
        )
    )
}
