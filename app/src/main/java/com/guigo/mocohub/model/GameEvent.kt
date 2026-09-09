package com.guigo.mocohub.model

data class GameEvent(
    val name: String,
    val timeLabel: String,
    val etaMillis: Long
) {
    val typeKey: String
        get() = when {
            name.contains("overcharged", ignoreCase = true) -> "overcharged"
            name.contains("chaos", ignoreCase = true) -> "chaos"
            else -> "other"
        }

    val stableKey: String
        get() = "${typeKey}_${timeLabel.replace(":", "-").replace(" ", "_")}"
}
