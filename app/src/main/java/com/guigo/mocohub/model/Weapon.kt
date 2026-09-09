package com.guigo.mocohub.model

data class Weapon(
    val name: String,
    val imageAsset: String?,   // caminho dentro de assets/weapons/, ou null pra usar um ícone genérico
    val skills: List<String>
)
