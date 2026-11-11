package fr.damien.zytho.data

import java.util.Locale

/** Référentiels : styles, formats, arômes, conseils de garde. */
object Beers {
    val STYLES = listOf(
        "Blonde", "Ambrée", "Brune", "Dubbel", "Tripel", "Quadrupel",
        "Strong Golden Ale", "Saison", "Witbier", "Gueuze", "Lambic", "Kriek",
        "Scotch", "Barley wine", "IPA", "Stout", "Autre",
    )

    private val GARDE = mapOf(
        "Quadrupel" to 5, "Dubbel" to 3, "Tripel" to 2, "Strong Golden Ale" to 2,
        "Brune" to 2, "Scotch" to 3, "Gueuze" to 10, "Lambic" to 5, "Kriek" to 3,
        "Barley wine" to 8, "Stout" to 3,
    )

    /** Durée de garde conseillée (années) selon le style. */
    fun gardeYears(style: String): Int = GARDE[style] ?: 1

    val PLACES = listOf("Maison", "Bar / brasserie", "Entre potes")
    val SERVICES = listOf("Bouteille", "Pression", "Canette")
    val FORMATS = listOf("25 cl", "33 cl", "37,5 cl", "75 cl", "Fût")
    val REBUY = listOf("Oui", "Peut-être", "Non")

    val DEFAULT_AROMAS: List<Aroma> = buildList {
        fun fam(family: String, vararg labels: String) = labels.forEach { add(Aroma(it, family)) }
        fam("Fruits", "Banane", "Agrumes", "Pomme", "Poire", "Abricot", "Pêche", "Cerise",
            "Fruits rouges", "Prune", "Raisin sec", "Figue", "Datte")
        fam("Épices", "Clou de girofle", "Poivre", "Coriandre", "Cannelle", "Réglisse", "Anis")
        fam("Malt", "Pain", "Biscuit", "Caramel", "Toffee", "Miel", "Sucre candi",
            "Chocolat", "Café", "Torréfié", "Noisette")
        fam("Levure", "Levuré", "Fruité estérifié", "Phénolé")
        fam("Houblon", "Herbacé", "Floral", "Résineux", "Amer", "Tropical")
        fam("Autres", "Alcool chaud", "Vineux", "Porto", "Boisé", "Vanille", "Acidulé",
            "Funky / Brett", "Cuir", "Tabac")
    }

    fun fmtNote(r: Double): String =
        if (r % 1.0 == 0.0) r.toInt().toString() else String.format(Locale.FRANCE, "%.1f", r)

    fun fmtAbv(a: Double?): String =
        when {
            a == null -> ""
            a % 1.0 == 0.0 -> a.toInt().toString()
            else -> String.format(Locale.FRANCE, "%.1f", a)
        }

    fun parseAbv(s: String): Double? = s.trim().replace(',', '.').replace("%", "").trim().toDoubleOrNull()
}
