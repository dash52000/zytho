package fr.damien.zytho.data

import android.content.Context
import java.text.Normalizer

/**
 * Une bière du catalogue d'Anselme (assets/candidates.csv).
 * Profils de 0 à 4, sur les mêmes échelles que la dégustation guidée.
 */
data class Candidate(
    val name: String,
    val brewery: String,
    val country: String,
    val region: String,
    val style: String,
    val abv: Double?,
    val ibu: Int?,
    val sweet: Int,
    val bitter: Int,
    val body: Int,
    /** "courante", "locale" ou "rare" */
    val availability: String,
    val trappist: Boolean,
    /** Pourquoi elle est placée là (colonne « ajustement ») */
    val note: String,
) {
    val key: String get() = Catalog.normalize(name)
    val rare: Boolean get() = availability == "rare"
    val local: Boolean get() = availability == "locale"
}

/** Lecture et recherche dans le catalogue embarqué. */
object Catalog {
    private const val FILE = "candidates.csv"

    fun load(context: Context): List<Candidate> =
        runCatching {
            context.assets.open(FILE).bufferedReader(Charsets.UTF_8).use { parse(it.readText()) }
        }.getOrDefault(emptyList())

    fun parse(text: String): List<Candidate> {
        val lines = text.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()
        val header = splitCsv(lines.first())
        fun idx(col: String) = header.indexOf(col)
        val iName = idx("nom"); val iBrew = idx("brasserie"); val iCountry = idx("pays")
        val iRegion = idx("region"); val iStyle = idx("style"); val iAbv = idx("degre")
        val iIbu = idx("ibu"); val iSw = idx("douceur"); val iBi = idx("amertume")
        val iBo = idx("corps"); val iAv = idx("disponibilite"); val iTr = idx("trappiste")
        val iNote = idx("ajustement")
        return lines.drop(1).mapNotNull { line ->
            val c = splitCsv(line)
            fun at(i: Int) = c.getOrNull(i)?.trim().orEmpty()
            val name = at(iName)
            if (name.isEmpty()) return@mapNotNull null
            Candidate(
                name = name,
                brewery = at(iBrew),
                country = at(iCountry),
                region = at(iRegion),
                style = at(iStyle).ifBlank { "Autre" },
                abv = at(iAbv).replace(',', '.').toDoubleOrNull(),
                ibu = at(iIbu).toIntOrNull(),
                sweet = (at(iSw).toIntOrNull() ?: 2).coerceIn(0, 4),
                bitter = (at(iBi).toIntOrNull() ?: 2).coerceIn(0, 4),
                body = (at(iBo).toIntOrNull() ?: 2).coerceIn(0, 4),
                availability = at(iAv).ifBlank { "courante" },
                trappist = at(iTr).equals("oui", ignoreCase = true),
                note = at(iNote),
            )
        }
    }

    /** Découpe une ligne CSV (virgule, guillemets doubles). */
    fun splitCsv(line: String): List<String> {
        val out = mutableListOf<String>()
        val sb = StringBuilder()
        var quoted = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                quoted && ch == '"' && i + 1 < line.length && line[i + 1] == '"' -> { sb.append('"'); i++ }
                ch == '"' -> quoted = !quoted
                ch == ',' && !quoted -> { out += sb.toString(); sb.clear() }
                else -> sb.append(ch)
            }
            i++
        }
        out += sb.toString()
        return out
    }

    /** Mots ignorés : articles et mots génériques des noms de brasserie. */
    private val ARTICLES = setOf(
        "la", "le", "les", "l", "de", "des", "du", "d",
        "brasserie", "brouwerij", "bierbrouwerij", "brewery", "artisanale",
    )

    /** Clé de comparaison : minuscules, sans accents ni ponctuation ni articles. */
    fun normalize(s: String): String {
        val noAccent = Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace("œ", "oe")
        return noAccent.split(Regex("[^a-z0-9]+"))
            .filter { it.isNotEmpty() && it !in ARTICLES }
            .joinToString(" ")
    }

    /** La bière du catalogue qui correspond à un nom saisi, ou null. */
    fun find(catalog: List<Candidate>, name: String, brewery: String = ""): Candidate? {
        val k = normalize(name)
        if (k.length < 3) return null
        catalog.firstOrNull { it.key == k }?.let { return it }
        // « Triple » + brasserie « Rulles » -> « La Rulles Triple »
        val withBrewery = normalize("$brewery $name")
        catalog.firstOrNull { it.key == withBrewery }?.let { return it }
        // Nom partiel, seulement s'il est assez précis et sans ambiguïté
        if (k.length < 5) return null
        val partial = catalog.filter { it.key.contains(k) || k.contains(it.key) }
        return partial.singleOrNull()
    }

    /** Suggestions pour l'autocomplétion de la saisie. */
    fun search(catalog: List<Candidate>, query: String, limit: Int = 3): List<Candidate> {
        val q = normalize(query)
        if (q.length < 2) return emptyList()
        val words = q.split(" ")
        return catalog
            .filter { c ->
                val hay = c.key + " " + normalize(c.brewery)
                words.all { w -> hay.split(" ").any { it.startsWith(w) } }
            }
            .sortedWith(compareByDescending<Candidate> { it.key.startsWith(q) }.thenBy { it.name.length })
            .take(limit)
    }
}
