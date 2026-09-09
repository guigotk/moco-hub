package com.guigo.mocohub.data

import android.content.Context
import com.guigo.mocohub.model.Weapon
import org.json.JSONArray

/**
 * Carrega a lista de armas de um arquivo local (assets/weapons.json),
 * em vez de baixar da internet — como o jogo não muda as armas com
 * frequência, faz mais sentido manter isso no próprio app e atualizar
 * a cada nova versão do app do que ficar fazendo scraping de outra página.
 *
 * Para adicionar/editar armas: edite app/src/main/assets/weapons.json.
 * Formato de cada item:
 * {
 *   "name": "Nome da arma",
 *   "image": "nome_do_arquivo.png"  (opcional, dentro de assets/weapons/),
 *   "skills": ["Skill 1", "Skill 2"]
 * }
 */
object WeaponsRepository {

    fun loadWeapons(context: Context): List<Weapon> {
        return try {
            val json = context.assets.open("weapons.json").bufferedReader().use { it.readText() }
            val array = JSONArray(json)
            val list = mutableListOf<Weapon>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val skillsArray = obj.optJSONArray("skills")
                val skills = mutableListOf<String>()
                if (skillsArray != null) {
                    for (j in 0 until skillsArray.length()) {
                        skills.add(skillsArray.getString(j))
                    }
                }
                list.add(
                    Weapon(
                        name = obj.optString("name"),
                        imageAsset = obj.optString("image").ifBlank { null },
                        skills = skills
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }
}
