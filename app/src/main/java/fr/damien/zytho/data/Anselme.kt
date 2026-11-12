package fr.damien.zytho.data

import kotlin.math.abs

/** Frère Anselme, moine brasseur : textes de guidage et retours après dégustation. */
object Anselme {
    const val NAME = "Frère Anselme"

    private val aromaFamily: Map<String, String> by lazy {
        Beers.DEFAULT_AROMAS.associate { it.label to it.family }
    }
    private val familyByExpress: Map<String, String> by lazy {
        StyleGuide.FAMILY_LABEL.entries.associate { (k, v) -> v to k }
    }

    fun familyOf(aroma: String): String? = aromaFamily[aroma] ?: familyByExpress[aroma]

    fun listFr(items: Collection<String>): String {
        val l = items.map { it.lowercase() }
        return when (l.size) {
            0 -> ""
            1 -> l[0]
            else -> l.dropLast(1).joinToString(", ") + " et " + l.last()
        }
    }

    private fun article(style: String): String = when (style) {
        "Blonde", "Ambrée", "Brune", "Saison", "Gueuze", "Kriek", "Strong Golden Ale", "IPA" -> "une"
        else -> "une"
    }

    // ---------- Conseils pendant la dégustation ----------

    fun tipEye(s: StyleInfo?): String =
        if (s == null || s.robe.isBlank()) "Tiens ton verre devant une lumière. Regarde la couleur, la clarté, la tenue de la mousse : l'œil prépare le nez."
        else "${article(s.name).replaceFirstChar { it.uppercase() }} ${s.name} se présente en général d'une robe ${s.robe.lowercase()}. Regarde aussi la mousse : fine et durable, c'est bon signe."

    fun tipNose(s: StyleInfo?): String =
        if (s == null) "Fais tourner doucement le verre, puis respire en deux temps : un premier coup bref, un second plus long. Note la première chose qui te vient, même si elle te paraît bizarre."
        else "Dans ${article(s.name)} ${s.name}, cherche : " +
            s.expected.take(4).joinToString(", ") { "${StyleGuide.icon(it)} ${it.lowercase()}" } +
            ". Mais ton nez a le dernier mot."

    fun tipMouth(s: StyleInfo?): String {
        if (s == null) return "Garde la gorgée quelques secondes : le sucré se sent sur le bout de la langue, l'amer en fond de bouche."
        val sweet = listOf("très sèche", "plutôt sèche", "équilibrée", "plutôt douce", "très douce")[s.sweet]
        val bitter = listOf("sans amertume", "peu amère", "moyennement amère", "amère", "très amère")[s.bitter]
        val body = listOf("très légère", "légère", "moyenne", "ronde", "épaisse")[s.body]
        return "On attend ${article(s.name)} ${s.name} $sweet, $bitter, au corps $body. Compare avec ce que tu ressens."
    }

    fun tipFinish(s: StyleInfo?): String =
        "Après avoir avalé, compte les secondes : combien de temps le goût reste-t-il ? " +
            (if (s != null && s.body >= 3) "Une ${s.name} laisse souvent une finale longue et chaleureuse." else "Une finale courte n'est pas un défaut : c'est souvent le signe d'une bière désaltérante.")

    // ---------- Retour après dégustation ----------

    fun feedback(t: Tasting, s: StyleInfo?, beerName: String): List<String> {
        val found = t.aromas.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val out = mutableListOf<String>()

        if (s == null) {
            out += "Je ne connais pas encore le style de « $beerName ». Indique-le dans sa fiche : je pourrai mieux te guider la prochaine fois."
        } else {
            val matched = found.filter { it in s.expected }
            val expectedFamilies = s.expected.mapNotNull { familyOf(it) }.toSet()
            val foundFamilies = found.mapNotNull { familyOf(it) }.toSet()
            val famMatch = foundFamilies intersect expectedFamilies
            when {
                matched.isNotEmpty() ->
                    out += "Bien vu pour ${listFr(matched)} : c'est la signature d'${article(s.name)} ${s.name}."
                famMatch.isNotEmpty() ->
                    out += "Tu as senti le côté ${listFr(famMatch.map { StyleGuide.FAMILY_LABEL[it] ?: it })} : c'est juste, pour ${article(s.name)} ${s.name}."
                found.isEmpty() ->
                    out += "Pas d'arôme noté cette fois, et c'est très bien ainsi. Quand tu en auras envie : prends une gorgée bouche fermée, puis souffle doucement par le nez. C'est là que les arômes se révèlent."
                else ->
                    out += "Tu as relevé ${listFr(found.take(3))}. Ton nez a sa propre idée, garde-la : les manuels attendent plutôt ${listFr(s.expected.take(3))} dans ${article(s.name)} ${s.name}."
            }
            val missing = s.expected.firstOrNull { it !in found }
            if (missing != null) {
                val why = StyleGuide.AROMA_WHY[missing]
                out += "La prochaine fois, cherche ${StyleGuide.icon(missing)} ${missing.lowercase()}" +
                    if (why != null) " : $why" else "."
            }
            val sw = t.sweetness
            val bi = t.bitterness
            if (sw != null && abs(sw - s.sweet) >= 2) {
                out += if (sw > s.sweet) "Tu l'as trouvée plus douce que la plupart des ${s.name} : une version plus ronde, ou une bouteille qui a pris de l'âge."
                else "Tu l'as trouvée plus sèche que la moyenne des ${s.name} : la levure a sans doute bien travaillé."
            } else if (bi != null && abs(bi - s.bitter) >= 2) {
                out += if (bi > s.bitter) "Plus amère que la moyenne du style : le brasseur a eu la main lourde sur le houblon."
                else "Moins amère que la moyenne du style : une version douce, tout en rondeur."
            }
        }

        out += when {
            t.rating >= 8.5 -> "Une belle rencontre. Retiens-la : elle en dit long sur ton palais."
            t.rating >= 7 -> "Une bonne bière, que tu reboiras sans doute avec plaisir."
            t.rating >= 5 -> "Correcte, sans plus. C'est utile aussi : on apprend par comparaison."
            else -> "Pas pour toi, et c'est précieux : savoir ce qu'on n'aime pas, c'est déjà s'y connaître."
        }
        if (s != null && s.facts.isNotEmpty()) {
            out += "Le savais-tu ? " + s.facts[(abs(t.id) % s.facts.size).toInt()]
        }
        return out
    }

    /** Lecture de la carte du goût. x : -4 (sucré) à +4 (amer) ; y : 0 (léger) à 4 (corsé). */
    fun palateReading(x: Float, y: Float, n: Int): String {
        if (n < 3) return "Encore quelques dégustations, et ton palais commencera à se dessiner ici."
        val side = when {
            x < -1f -> "les bières douces et maltées"
            x > 1f -> "les bières sèches et amères"
            else -> "l'équilibre entre douceur et amertume"
        }
        val weight = when {
            y >= 2.7f -> "avec du corps"
            y <= 1.3f -> "légères et vives"
            else -> "au corps moyen"
        }
        val hint = when {
            x < -1f && y >= 2.7f -> "Les Dubbels, Quads et Scotch sont ta maison."
            x > 1f && y <= 1.3f -> "Les Saisons, Strong Golden et Gueuzes devraient te plaire."
            x > 1f -> "Les IPA et les Stouts sont faits pour toi."
            y <= 1.3f -> "Blanches et Tripels te vont bien."
            else -> "Blondes, Ambrées et Brunes sont un terrain sûr ; ose un pas de côté de temps en temps."
        }
        return "Tes bières préférées penchent vers $side, $weight. $hint"
    }
}

/** Parcours d'initiation : 3 étapes fixes, puis adaptation aux goûts. */
data class ParcoursStep(val style: StyleInfo, val reason: String)

object Parcours {
    val START = listOf("Blonde", "Tripel", "Dubbel")

    private val NEXT: Map<String, List<String>> = mapOf(
        "Blonde" to listOf("Tripel", "Saison", "Witbier", "Ambrée"),
        "Tripel" to listOf("Strong Golden Ale", "Saison", "Blonde"),
        "Strong Golden Ale" to listOf("Tripel", "Saison", "IPA"),
        "Dubbel" to listOf("Quadrupel", "Brune", "Scotch", "Ambrée"),
        "Brune" to listOf("Dubbel", "Scotch", "Stout"),
        "Quadrupel" to listOf("Barley wine", "Scotch", "Stout"),
        "Ambrée" to listOf("Dubbel", "Scotch", "Blonde"),
        "Saison" to listOf("Witbier", "Gueuze", "Strong Golden Ale"),
        "Witbier" to listOf("Saison", "Kriek", "Gueuze"),
        "Gueuze" to listOf("Lambic", "Kriek"),
        "Kriek" to listOf("Gueuze", "Lambic"),
        "Lambic" to listOf("Gueuze", "Kriek"),
        "Scotch" to listOf("Quadrupel", "Barley wine"),
        "Barley wine" to listOf("Quadrupel", "Stout"),
        "IPA" to listOf("Strong Golden Ale", "Saison", "Barley wine"),
        "Stout" to listOf("Quadrupel", "Scotch", "Barley wine"),
    )

    /** Styles déjà validés, avec la date de première dégustation. */
    fun done(beers: List<Beer>, tastings: List<Tasting>): List<Pair<StyleInfo, Long>> {
        val styleById = beers.associate { it.id to it.style }
        return tastings
            .mapNotNull { t -> styleById[t.beerId]?.let { st -> StyleGuide.of(st)?.let { it to t.date } } }
            .filter { it.first.name != "Autre" }
            .groupBy { it.first.name }
            .map { (_, list) -> list.minBy { it.second } }
            .sortedBy { it.second }
    }

    /** Styles proposés comme point de départ. */
    val STARTS: List<String> = StyleGuide.ALL.map { it.name }.filter { it != "Autre" }

    fun next(beers: List<Beer>, tastings: List<Tasting>, start: String = "Tripel"): ParcoursStep? {
        val doneNames = done(beers, tastings).map { it.first.name }.toSet()
        if (start !in doneNames) {
            StyleGuide.of(start)?.let {
                return ParcoursStep(it, "Ton point de départ : c'est avec elle que je fais connaissance avec ton palais.")
            }
        }
        val styleById = beers.associate { it.id to it.style }
        val avgByStyle = tastings
            .mapNotNull { t -> styleById[t.beerId]?.let { it to t.rating } }
            .groupBy({ it.first }, { it.second })
            .mapValues { it.value.average() }
            .entries.sortedByDescending { it.value }
        for ((style, avg) in avgByStyle) {
            if (avg < 6.0) continue
            val candidate = NEXT[style].orEmpty().firstOrNull { it !in doneNames } ?: continue
            val s = StyleGuide.of(candidate) ?: continue
            return ParcoursStep(s, "Parce que tu as aimé la $style (${Beers.fmtNote(Math.round(avg * 10) / 10.0)}/10).")
        }
        val order = START + StyleGuide.ALL.map { it.name }.filter { it !in START && it != "Autre" }
        val rest = order.firstOrNull { it !in doneNames }?.let { StyleGuide.of(it) } ?: return null
        return ParcoursStep(rest, "Pour élargir ton horizon.")
    }
}
