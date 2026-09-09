package com.guigo.mocohub.model

data class PlayerStats(
    val name: String,
    val tag: String,
    val careerLevel: String = "—",
    val currentLevel: String = "—",
    val collectorLevel: String = "—",
    val eliteRank: String = "—",
    val eliteContracts: String = "—",
    val rating: String = "—",
    val eliteScore: Int? = null,
    val grindScore: Int? = null,
    val progressionScore: Int? = null,
    val collectionScore: Int? = null,
    val title: String = "",
    val eliteMerits: String = "—",
    val careerTopPercent: Int? = null,
    val collectorTopPercent: Int? = null,
    val eliteMeritsTopPercent: Int? = null,
    val lastUpdated: String = ""
)
