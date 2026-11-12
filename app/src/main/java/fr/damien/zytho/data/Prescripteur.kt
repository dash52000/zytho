package fr.damien.zytho.data

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sqrt

/** Les trois sortes de conseils d'Anselme. */
enum class RecoKind(val emoji: String, val title: String) {
    NEAR("🎯", "Dans tes cordes"),
    SIDE("🧭", "Un pas de côté"),
    RARE("💎", "Chasse au trésor"),
}

/** Un conseil affiché : la bière, sa position (éventuellement recalée) et le pourquoi. */
data class Reco(
    val kind: RecoKind,
    val candidate: Candidate,
    val x: Float,
    val y: Float,
    val why: List<String>,
    /** Il existe au moins une autre idée pour ce type de conseil */
    val hasAlternative: Boolean,
)

/** Recalage d'un style sur un axe, appris des dégustations. */
data class StyleShift(val style: String, val axis: Axis, val delta: Int)

enum class Axis { SWEET, BITTER, BODY }

/** Position d'une bière du catalogue sur la carte, recalages compris. */
data class Placed(val candidate: Candidate, val sweet: Int, val bitter: Int, val body: Int, val shifted: Boolean) {
    val x: Float get() = (bitter - sweet).toFloat()
    val y: Float get() = body.toFloat()
}

/** Ce que la page Palais doit afficher pour les conseils. */
data class Prescription(
    /** Null tant qu'Anselme ne peut pas encore conseiller */
    val recos: List<Reco>,
    /** Message d'attente, ou lecture des recalages */
    val intro: List<String>,
    /** Bières du catalogue pas encore goûtées : points fantômes de la carte */
    val ghosts: List<Placed>,
)

/**
 * Anselme prescripteur : choisit des bières du catalogue à partir de la carte du goût.
 * Logique pure, sans Android, pour être testable.
 */
object Prescripteur {
    const val MIN_TASTINGS = 3
    private const val SIDE_MIN = 1.5f
    private const val SIDE_MAX = 3.5f
    private const val RARE_MAX = 2.0f

    // ---------- Correspondance entre tes bières et le catalogue ----------

    /** Associe chaque bière de ton carnet à une bière du catalogue, quand c'est possible. */
    fun matches(catalog: List<Candidate>, beers: List<Beer>, breweries: List<Brewery>): Map<Long, Candidate> {
        val brById = breweries.associateBy { it.id }
        return beers.mapNotNull { b ->
            val brewery = b.breweryId?.let { brById[it]?.name }.orEmpty()
            Catalog.find(catalog, b.name, brewery)?.let { b.id to it }
        }.toMap()
    }

    // ---------- Recalage par style ----------

    /**
     * Un style est recalé sur un axe quand au moins 2 dégustations de bières du catalogue
     * de ce style s'écartent de la fiche dans le même sens (et aucune dans l'autre).
     */
    fun shifts(catalog: List<Candidate>, beers: List<Beer>, breweries: List<Brewery>, tastings: List<Tasting>): List<StyleShift> {
        val byBeer = matches(catalog, beers, breweries)
        val diffs = mutableMapOf<Pair<String, Axis>, MutableList<Int>>()
        for (t in tastings) {
            val c = byBeer[t.beerId] ?: continue
            t.sweetness?.let { diffs.getOrPut(c.style to Axis.SWEET) { mutableListOf() } += it - c.sweet }
            t.bitterness?.let { diffs.getOrPut(c.style to Axis.BITTER) { mutableListOf() } += it - c.bitter }
            t.body?.let { diffs.getOrPut(c.style to Axis.BODY) { mutableListOf() } += it - c.body }
        }
        return diffs.mapNotNull { (k, list) ->
            val gaps = list.filter { abs(it) >= 1 }
            if (gaps.size < 2) return@mapNotNull null
            val s = gaps.first().sign
            if (gaps.any { it.sign != s }) return@mapNotNull null
            val delta = gaps.average().roundToInt().coerceIn(-2, 2)
            if (delta == 0) null else StyleShift(k.first, k.second, delta)
        }
    }

    fun place(c: Candidate, shifts: List<StyleShift>): Placed {
        val mine = shifts.filter { it.style == c.style }
        fun d(axis: Axis) = mine.firstOrNull { it.axis == axis }?.delta ?: 0
        return Placed(
            candidate = c,
            sweet = (c.sweet + d(Axis.SWEET)).coerceIn(0, 4),
            bitter = (c.bitter + d(Axis.BITTER)).coerceIn(0, 4),
            body = (c.body + d(Axis.BODY)).coerceIn(0, 4),
            shifted = mine.isNotEmpty(),
        )
    }

    // ---------- Classements ----------

    private fun dist(ax: Float, ay: Float, bx: Float, by: Float): Float {
        val dx = ax - bx
        val dy = ay - by
        return sqrt(dx * dx + dy * dy)
    }

    /** Styles que tu aimes : moyenne de 7/10 ou plus. */
    fun likedStyles(beers: List<Beer>, tastings: List<Tasting>): Set<String> {
        val styleById = beers.associate { it.id to it.style }
        return tastings.mapNotNull { t -> styleById[t.beerId]?.takeIf { it.isNotBlank() }?.let { it to t.rating } }
            .groupBy({ it.first }, { it.second })
            .filterValues { it.average() >= 7.0 }
            .keys
    }

    /** Pénalité pour « Dans tes cordes » quand le style n'est pas un de ceux que tu aimes. */
    private const val OTHER_STYLE_PENALTY = 0.75f

    /** Candidates classées pour un type de conseil, de la meilleure à la moins bonne. */
    fun ranked(
        kind: RecoKind,
        pool: List<Placed>,
        center: Pair<Float, Float>,
        points: List<Pair<Float, Float>>,
        liked: Set<String> = emptySet(),
    ): List<Placed> {
        val (cx, cy) = center
        fun d(p: Placed) = dist(p.x, p.y, cx, cy)
        // La carte ne garde que deux axes : le style aimé départage les bières au même endroit
        fun near(p: Placed) = d(p) + if (liked.isEmpty() || p.candidate.style in liked) 0f else OTHER_STYLE_PENALTY
        return when (kind) {
            RecoKind.NEAR -> pool.filter { !it.candidate.rare }
                .sortedWith(compareBy<Placed> { near(it) }.thenByDescending { it.candidate.local })
            RecoKind.SIDE -> {
                val common = pool.filter { !it.candidate.rare }
                val band = common.filter { d(it) in SIDE_MIN..SIDE_MAX }
                    .ifEmpty { common.filter { d(it) >= SIDE_MIN }.sortedBy { d(it) }.take(8) }
                // Le moins exploré d'abord : loin de tout ce que tu as déjà bu
                band.sortedByDescending { p ->
                    points.minOfOrNull { (px, py) -> dist(p.x, p.y, px, py) } ?: 0f
                }
            }
            RecoKind.RARE -> pool.filter { it.candidate.rare && d(it) <= RARE_MAX }.sortedBy { near(it) }
        }
    }

    /** Garde le conseil en cours s'il est encore valable, sinon prend le premier du classement. */
    fun choose(ranked: List<Placed>, currentKey: String?): Placed? =
        ranked.firstOrNull { it.candidate.key == currentKey } ?: ranked.firstOrNull()

    /** « Autre idée » : la suivante dans le classement, en boucle. */
    fun nextKey(ranked: List<Placed>, currentKey: String?): String? {
        if (ranked.isEmpty()) return null
        val i = ranked.indexOfFirst { it.candidate.key == currentKey }
        return ranked[(i + 1) % ranked.size].candidate.key
    }

    // ---------- Assemblage ----------

    /**
     * @param points points de ta carte du goût (x, y)
     * @param center centre de tes bières aimées, ou null
     * @param current conseils figés en cours, par type
     * @param skip bières à ne pas proposer : exclues (« Pas pour moi ») et déjà en wishlist
     */
    fun prescribe(
        catalog: List<Candidate>,
        beers: List<Beer>,
        breweries: List<Brewery>,
        tastings: List<Tasting>,
        points: List<Pair<Float, Float>>,
        center: Pair<Float, Float>?,
        current: Map<RecoKind, String?>,
        skip: Set<String>,
        nextStep: ParcoursStep?,
    ): Prescription {
        val tastedIds = tastings.map { it.beerId }.toSet()
        val tastedKeys = matches(catalog, beers.filter { it.id in tastedIds }, breweries).values.map { it.key }.toSet()
        val shifts = shifts(catalog, beers, breweries, tastings)
        val untasted = catalog.filter { it.key !in tastedKeys }.map { place(it, shifts) }

        if (points.size < MIN_TASTINGS || center == null) {
            return Prescription(emptyList(), waiting(points.size, center == null, nextStep), untasted)
        }

        val pool = untasted.filter { it.candidate.key !in skip }
        val liked = likedStyles(beers, tastings)
        val recos = mutableListOf<Reco>()
        val used = mutableSetOf<String>()
        for (kind in RecoKind.entries) {
            val list = ranked(kind, pool.filter { it.candidate.key !in used }, center, points, liked)
            val p = choose(list, current[kind]) ?: continue
            used += p.candidate.key
            recos += Reco(kind, p.candidate, p.x, p.y, why(kind, p, center, shifts), list.size > 1)
        }
        return Prescription(recos, shiftLines(shifts), untasted)
    }

    // ---------- Les mots d'Anselme ----------

    private val SWEET = listOf("très sèche", "plutôt sèche", "équilibrée", "plutôt douce", "très douce")
    private val BITTER = listOf("sans amertume", "peu amère", "moyennement amère", "amère", "très amère")
    private val BODY = listOf("très léger", "léger", "moyen", "rond", "épais")

    private fun waiting(n: Int, noLiked: Boolean, step: ParcoursStep?): List<String> {
        val first = if (n < MIN_TASTINGS) {
            val left = MIN_TASTINGS - n
            "Note encore $left dégustation${if (left > 1) "s" else ""} et je te conseillerai des bières précises, choisies pour ton palais."
        } else if (noLiked) {
            "Tu n'as encore noté aucune bière 7/10 ou plus : je ne sais pas encore ce qui te plaît vraiment."
        } else ""
        val out = mutableListOf(first)
        if (step != null) {
            out += "En attendant, ta prochaine étape du parcours, c'est la ${step.style.name} : commence par la ${step.style.classic}." +
                (step.style.local?.let { " Côté local : $it." } ?: "")
        }
        return out.filter { it.isNotBlank() }
    }

    private fun axisWord(axis: Axis, delta: Int): String = when (axis) {
        Axis.SWEET -> if (delta > 0) "plus douces" else "plus sèches"
        Axis.BITTER -> if (delta > 0) "plus amères" else "moins amères"
        Axis.BODY -> if (delta > 0) "plus corsées" else "plus légères"
    }

    fun shiftLines(shifts: List<StyleShift>): List<String> =
        shifts.groupBy { it.style }.map { (style, list) ->
            val words = Anselme.listFr(list.map { axisWord(it.axis, it.delta) })
            "Tu trouves les $style $words que prévu : j'ai recalé toutes les autres $style de mon catalogue."
        }

    private fun localLine(c: Candidate): String = when {
        c.region == "Gaume" -> "Et elle est brassée en Gaume, tout près de chez toi."
        c.local -> "Et elle est brassée en ${c.region}, à deux pas."
        else -> ""
    }

    private fun noteLine(c: Candidate): String =
        if (c.note.isBlank() || c.note.contains("ancrage", ignoreCase = true)) ""
        else "Ce qui la distingue : ${c.note}."

    private fun why(kind: RecoKind, p: Placed, center: Pair<Float, Float>, shifts: List<StyleShift>): List<String> {
        val c = p.candidate
        val out = mutableListOf<String>()
        when (kind) {
            RecoKind.NEAR -> out += "Une ${c.style} ${SWEET[p.sweet]}, ${BITTER[p.bitter]}, au corps ${BODY[p.body]} : pile dans la zone de tes bières préférées."
            RecoKind.SIDE -> {
                val dx = p.x - center.first
                val dy = p.y - center.second
                val dirs = listOfNotNull(
                    when { dx > 0.8f -> "plus amère"; dx < -0.8f -> "plus douce"; else -> null },
                    when { dy > 0.8f -> "plus corsée"; dy < -0.8f -> "plus légère"; else -> null },
                )
                val keep = when {
                    abs(dx) <= 0.8f -> " Tu y retrouveras l'équilibre sucré-amer que tu aimes."
                    abs(dy) <= 0.8f -> " Tu y retrouveras le corps que tu aimes."
                    else -> ""
                }
                out += "Une ${c.style} ${Anselme.listFr(dirs).ifBlank { "un peu différente" }} que ce que tu bois d'habitude, sans t'éloigner trop de ton palais.$keep"
            }
            RecoKind.RARE -> out += "Si un jour tu en croises une, ne la laisse pas passer : elle tombe dans ta zone."
        }
        if (c.trappist) out += "Une vraie trappiste, brassée par des moines."
        out += localLine(c)
        out += noteLine(c)
        if (p.shifted) {
            shifts.firstOrNull { it.style == c.style }?.let {
                out += "Je l'ai recalée : tu trouves les ${c.style} ${axisWord(it.axis, it.delta)} que les fiches."
            }
        }
        return out.filter { it.isNotBlank() }
    }

    /** Ce qu'Anselme dit dans la fiche souvenir quand tu goûtes une bière de son catalogue. */
    fun souvenir(
        t: Tasting,
        c: Candidate,
        recommendedAs: RecoKind?,
        shifts: List<StyleShift>,
    ): List<String> {
        val out = mutableListOf<String>()
        val note = Beers.fmtNote(t.rating)
        if (recommendedAs != null) {
            out += when {
                t.rating >= 7 -> "Je te l'avais conseillée, et tu lui mets $note/10 : on commence à bien se connaître, toi et moi."
                t.rating < 6 -> "Je pensais qu'elle te plairait, tu lui mets $note/10. Noté : j'en tiens compte pour la suite."
                else -> "Je te l'avais conseillée : $note/10, un avis partagé. Ça m'aide quand même à affiner."
            }
        }
        val gaps = listOfNotNull(
            t.sweetness?.let { Triple(Axis.SWEET, it - c.sweet, if (it > c.sweet) "plus douce" else "plus sèche") },
            t.bitterness?.let { Triple(Axis.BITTER, it - c.bitter, if (it > c.bitter) "plus amère" else "moins amère") },
            t.body?.let { Triple(Axis.BODY, it - c.body, if (it > c.body) "plus corsée" else "plus légère") },
        )
        if (gaps.isEmpty()) return out
        val off = gaps.filter { abs(it.second) >= 1 }
        if (off.isEmpty()) {
            out += "Ton palais confirme ma fiche : je l'avais bien placée."
        } else {
            out += "Tu la trouves ${Anselme.listFr(off.map { it.third })} que je ne l'imaginais."
            val confirmed = off.any { g -> shifts.any { it.style == c.style && it.axis == g.first } }
            out += if (confirmed) "Ce n'est pas la première ${c.style} dans ce cas : j'ai recalé toutes les autres."
            else "Si une autre ${c.style} te fait le même effet, je recalerai tout le style."
        }
        return out
    }
}
