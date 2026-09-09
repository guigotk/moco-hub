package com.guigo.mocohub.model

data class NewsItem(
    val title: String,
    val summary: String,
    val url: String,
    val source: String,
    val published: String = "",
    val imageUrl: String = ""
)
