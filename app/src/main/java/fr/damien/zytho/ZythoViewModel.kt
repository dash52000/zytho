package fr.damien.zytho

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import fr.damien.zytho.data.Aroma
import fr.damien.zytho.data.Backup
import fr.damien.zytho.data.Barcode
import fr.damien.zytho.data.Beer
import fr.damien.zytho.data.Beers
import fr.damien.zytho.data.Brewery
import fr.damien.zytho.data.Candidate
import fr.damien.zytho.data.Catalog
import fr.damien.zytho.data.CatalogExclusion
import fr.damien.zytho.data.CellarItem
import fr.damien.zytho.data.Geocoder
import fr.damien.zytho.data.OpenFoodFacts
import fr.damien.zytho.data.Parcours
import fr.damien.zytho.data.Place
import fr.damien.zytho.data.Prescription
import fr.damien.zytho.data.Prescripteur
import fr.damien.zytho.data.Reco
import fr.damien.zytho.data.RecoKind
import fr.damien.zytho.data.Tasting
import fr.damien.zytho.data.WishItem
import fr.damien.zytho.data.ZythoDb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Brouillon partagé entre le scan, le choix d'action et le formulaire. */
data class Draft(
    /** Renseigné quand on modifie une dégustation existante */
    val tastingId: Long? = null,
    val beerId: Long? = null,
    val name: String = "",
    val breweryName: String = "",
    val country: String = "",
    val style: String = "",
    val abv: String = "",
    val photoPath: String? = null,
    val rebuy: String = "",
    val barcode: String? = null,
    /** D'où vient la fiche : affiché à l'écran de choix */
    val source: String = "",
    // Dégustation
    val rating: Float = 7f,
    val aromas: Set<String> = emptySet(),
    val place: String = "Maison",
    val service: String = "Bouteille",
    val vintage: String = "",
    val format: String = "33 cl",
    val comment: String = "",
    val trappist: Boolean = false,
    // Dégustation guidée (facultatif)
    val color: String = "",
    val sweetness: Int? = null,
    val bitterness: Int? = null,
    val body: Int? = null,
    val fizz: Int? = null,
    val finish: String = "",
    // Cave
    val quantity: Int = 1,
    val drinkBefore: String = "",
    // Wishlist
    val wishNote: String = "",
)

class ZythoViewModel(private val app: Application) : AndroidViewModel(app) {
    private val db = ZythoDb.get(app)
    private val dao = db.dao()
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true; encodeDefaults = true }

    val breweries: StateFlow<List<Brewery>> = dao.breweries().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val beers: StateFlow<List<Beer>> = dao.beers().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val tastings: StateFlow<List<Tasting>> = dao.tastings().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val cellar: StateFlow<List<CellarItem>> = dao.cellar().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val wishes: StateFlow<List<WishItem>> = dao.wishes().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val barcodes: StateFlow<List<Barcode>> = dao.barcodes().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val aromas: StateFlow<List<Aroma>> = dao.aromas().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val exclusions: StateFlow<List<CatalogExclusion>> = dao.exclusions().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Catalogue d'Anselme : bières candidates embarquées dans l'appli. */
    val catalog: List<Candidate> by lazy { Catalog.load(app) }

    var draft by mutableStateOf(Draft())
    var busy by mutableStateOf(false)
    var lastTastingId: Long = 0

    /** « Encore une ? » : bières goûtées au moins deux fois, les plus fréquentes d'abord. */
    fun habitualBeers(): List<Beer> {
        val counts = tastings.value.groupingBy { it.beerId }.eachCount().filter { it.value >= 2 }
        val byId = beers.value.associateBy { it.id }
        return counts.entries.sortedByDescending { it.value }.mapNotNull { byId[it.key] }.take(3)
    }

    /** Prépare une dégustation express d'une bière habituelle, avec la dernière note. */
    suspend fun prepareAgain(beerId: Long) {
        prepareForBeer(beerId)
        val last = tastings.value.filter { it.beerId == beerId }.maxByOrNull { it.date }
        draft = draft.copy(
            rating = last?.rating?.toFloat() ?: draft.rating,
            place = last?.place?.ifBlank { null } ?: draft.place,
            service = last?.service?.ifBlank { null } ?: draft.service,
            format = last?.format?.ifBlank { null } ?: draft.format,
            source = "Encore une !",
        )
    }

    init {
        viewModelScope.launch { dao.insertAromas(Beers.DEFAULT_AROMAS) }
    }

    // ---------- Préparation des brouillons ----------

    fun startManual() {
        draft = Draft(source = "Saisie manuelle")
    }

    private suspend fun draftForBeer(beerId: Long): Draft {
        val beer = dao.beer(beerId) ?: return Draft()
        val brewery = beer.breweryId?.let { dao.brewery(it) }
        return Draft(
            beerId = beer.id,
            name = beer.name,
            breweryName = brewery?.name.orEmpty(),
            country = brewery?.country.orEmpty(),
            style = beer.style,
            abv = Beers.fmtAbv(beer.abv),
            photoPath = beer.photoPath,
            rebuy = beer.rebuy,
            trappist = brewery?.trappist ?: false,
        )
    }

    /** Charge une dégustation existante pour la modifier (bière, brasserie, arômes, tout). */
    suspend fun prepareEditTasting(tastingId: Long) {
        val t = dao.tasting(tastingId) ?: return
        val base = draftForBeer(t.beerId)
        draft = base.copy(
            tastingId = t.id,
            rating = t.rating.toFloat(),
            aromas = t.aromas.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet(),
            place = t.place.ifBlank { base.place },
            service = t.service.ifBlank { base.service },
            vintage = t.vintage?.toString().orEmpty(),
            format = t.format.ifBlank { base.format },
            comment = t.comment,
            color = t.color.orEmpty(),
            sweetness = t.sweetness,
            bitterness = t.bitterness,
            body = t.body,
            fizz = t.fizz,
            finish = t.finish.orEmpty(),
            source = "Modification de la dégustation du ${java.time.Instant.ofEpochMilli(t.date).atZone(ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}",
        )
    }

    // ---------- Parcours ----------

    private val prefs = app.getSharedPreferences("zytho", android.content.Context.MODE_PRIVATE)
    var parcoursStart by mutableStateOf(prefs.getString("parcours_start", "Tripel") ?: "Tripel")
        private set

    fun changeParcoursStart(style: String) {
        parcoursStart = style
        prefs.edit().putString("parcours_start", style).apply()
    }

    // ---------- Anselme prescripteur ----------

    /** Conseils figés en cours : ils ne changent que sur « Autre idée » (ou s'ils ne sont plus valables). */
    var recoKeys by mutableStateOf(
        RecoKind.entries.associateWith { prefs.getString("reco_${it.name}", null) }
    )
        private set

    /** Bières à ne pas proposer : écartées ou déjà dans la wishlist. */
    private fun recoSkip(): Set<String> {
        val wishIds = wishes.value.map { it.beerId }.toSet()
        val inWish = Prescripteur.matches(catalog, beers.value.filter { it.id in wishIds }, breweries.value).values.map { it.key }
        return exclusions.value.map { it.name }.toSet() + inWish
    }

    fun prescription(points: List<Pair<Float, Float>>, center: Pair<Float, Float>?): Prescription =
        Prescripteur.prescribe(
            catalog = catalog,
            beers = beers.value,
            breweries = breweries.value,
            tastings = tastings.value,
            points = points,
            center = center,
            current = recoKeys,
            skip = recoSkip(),
            nextStep = Parcours.next(beers.value, tastings.value, parcoursStart),
        )

    /** Fige les conseils affichés et garde la trace de ce qui a été conseillé (pour la fiche souvenir). */
    fun rememberRecos(recos: List<Reco>) {
        val keys = RecoKind.entries.associateWith { k -> recos.firstOrNull { it.kind == k }?.candidate?.key }
        if (keys == recoKeys) return
        recoKeys = keys
        val seen = prefs.getStringSet("reco_seen", emptySet()).orEmpty().toMutableSet()
        recos.forEach { seen += "${it.candidate.key}|${it.kind.name}" }
        prefs.edit().apply {
            keys.forEach { (k, v) -> putString("reco_${k.name}", v) }
            putStringSet("reco_seen", seen)
        }.apply()
    }

    /** Le type de conseil sous lequel Anselme a proposé cette bière, s'il l'a fait. */
    fun recommendedAs(key: String): RecoKind? =
        prefs.getStringSet("reco_seen", emptySet()).orEmpty()
            .filter { it.substringBefore('|') == key }
            .mapNotNull { runCatching { RecoKind.valueOf(it.substringAfter('|')) }.getOrNull() }
            .firstOrNull()

    fun otherIdea(kind: RecoKind, shown: List<Reco>, points: List<Pair<Float, Float>>, center: Pair<Float, Float>?) {
        if (center == null) return
        val others = shown.filter { it.kind != kind }.map { it.candidate.key }.toSet()
        val tastedIds = tastings.value.map { it.beerId }.toSet()
        val tastedKeys = Prescripteur.matches(catalog, beers.value.filter { it.id in tastedIds }, breweries.value).values.map { it.key }.toSet()
        val shifts = Prescripteur.shifts(catalog, beers.value, breweries.value, tastings.value)
        val skip = recoSkip() + others + tastedKeys
        val pool = catalog.filter { it.key !in skip }.map { Prescripteur.place(it, shifts) }
        val liked = Prescripteur.likedStyles(beers.value, tastings.value)
        val ranked = Prescripteur.ranked(kind, pool, center, points, liked)
        val current = shown.firstOrNull { it.kind == kind }?.candidate?.key
        val next = Prescripteur.nextKey(ranked, current) ?: return
        recoKeys = recoKeys + (kind to next)
        prefs.edit().putString("reco_${kind.name}", next).apply()
    }

    fun notForMe(reco: Reco) = viewModelScope.launch {
        dao.insertExclusion(CatalogExclusion(reco.candidate.key, System.currentTimeMillis()))
    }

    /** Ajoute une bière conseillée à la wishlist (en réutilisant sa fiche si elle existe déjà). */
    fun addRecoToWish(reco: Reco) = viewModelScope.launch {
        val c = reco.candidate
        val existing = beers.value.firstOrNull { Catalog.normalize(it.name) == c.key }
        val note = "${reco.kind.emoji} Conseil de Frère Anselme"
        if (existing != null) {
            if (wishes.value.none { it.beerId == existing.id }) {
                dao.insertWish(WishItem(beerId = existing.id, note = note, addedAt = System.currentTimeMillis()))
            }
        } else {
            save(fillFromCandidate(Draft(), c).copy(wishNote = note), "wish")
        }
    }

    /** Remplit la fiche avec les infos du catalogue (nom, brasserie, pays, style, degré). */
    fun fillFromCandidate(d: Draft, c: Candidate): Draft = d.copy(
        name = c.name,
        breweryName = c.brewery,
        country = c.country,
        style = c.style,
        abv = Beers.fmtAbv(c.abv).ifBlank { d.abv },
        trappist = c.trappist || d.trappist,
    )

    // ---------- Carte des brasseries ----------

    suspend fun searchPlaces(query: String): List<Place> = Geocoder.search(query)

    fun setBreweryLocation(b: Brewery, lat: Double?, lon: Double?) = viewModelScope.launch {
        dao.updateBrewery(b.copy(lat = lat, lon = lon))
    }

    suspend fun prepareForBeer(beerId: Long, vintage: Int? = null, format: String? = null) {
        val d = draftForBeer(beerId)
        draft = d.copy(
            vintage = vintage?.toString() ?: d.vintage,
            format = if (format.isNullOrBlank()) d.format else format,
        )
    }

    suspend fun resolveBarcode(code: String) {
        busy = true
        try {
            val known = dao.barcode(code)
            if (known != null && dao.beer(known.beerId) != null) {
                draft = draftForBeer(known.beerId).copy(
                    barcode = code,
                    format = known.format.ifBlank { "33 cl" },
                    source = "Bière reconnue",
                )
                return
            }
            val off = OpenFoodFacts.lookup(code)
            draft = if (off != null) {
                var photo: String? = null
                val img = off.imageUrl
                if (img != null) {
                    val dir = File(app.filesDir, "photos").apply { mkdirs() }
                    val file = File(dir, "off_${code}_${System.currentTimeMillis()}.jpg")
                    if (OpenFoodFacts.downloadImage(img, file)) photo = file.absolutePath
                }
                val filled = listOfNotNull(
                    "style".takeIf { off.style.isNotBlank() },
                    "format".takeIf { off.format.isNotBlank() },
                    "pays".takeIf { off.country.isNotBlank() },
                    "photo".takeIf { photo != null },
                )
                val inCatalog = Catalog.find(catalog, off.name, off.brand)
                Draft(
                    name = inCatalog?.name ?: off.name,
                    breweryName = inCatalog?.brewery ?: off.brand,
                    country = off.country.ifBlank { inCatalog?.country.orEmpty() },
                    style = inCatalog?.style ?: off.style,
                    abv = Beers.fmtAbv(off.abv ?: inCatalog?.abv),
                    format = off.format.ifBlank { "33 cl" },
                    trappist = off.trappist || inCatalog?.trappist == true,
                    photoPath = photo,
                    barcode = code,
                    source = "Trouvée sur Open Food Facts" +
                        (if (filled.isEmpty()) "" else " (${filled.joinToString(", ")} pré-remplis)") +
                        (if (inCatalog != null) ", reconnue par Anselme" else "") +
                        " : vérifie la fiche",
                )
            } else {
                Draft(barcode = code, source = "Code $code inconnu : remplis la fiche, il sera mémorisé")
            }
        } finally {
            busy = false
        }
    }

    // ---------- Photos et lecture d'étiquette ----------

    /** Crée un fichier photo vide et renvoie (uri pour l'appareil photo, chemin local). */
    fun newPhoto(): Pair<Uri, String> {
        val dir = File(app.filesDir, "photos").apply { mkdirs() }
        val file = File(dir, "biere_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", file)
        return uri to file.absolutePath
    }

    suspend fun draftFromLabel(path: String) {
        busy = true
        try {
            val base = Draft(photoPath = path, source = "Photo d'étiquette : vérifie ce qui a été lu")
            draft = try {
                val image = InputImage.fromFilePath(app, Uri.fromFile(File(path)))
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                val text = recognizer.process(image).awaitTask()
                val abvMatch = Regex("""(\d{1,2}(?:[.,]\d{1,2})?)\s*%""").find(text.text)
                val abv = abvMatch?.groupValues?.get(1)?.replace('.', ',') ?: ""
                val lines = text.textBlocks.flatMap { it.lines }
                val nameLine = lines
                    .filter { l -> !l.text.contains('%') && l.text.count { it.isLetter() } >= 3 }
                    .maxByOrNull { it.boundingBox?.height() ?: 0 }
                    ?.text.orEmpty()
                val read = base.copy(name = prettyName(nameLine), abv = abv)
                val known = Catalog.find(catalog, read.name)
                if (known != null) {
                    fillFromCandidate(read, known).copy(
                        abv = abv.ifBlank { Beers.fmtAbv(known.abv) },
                        source = "Photo d'étiquette : Anselme a reconnu la ${known.name}, vérifie la fiche",
                    )
                } else read
            } catch (e: Exception) {
                base.copy(source = "Photo enregistrée, lecture impossible : saisis la fiche")
            }
        } finally {
            busy = false
        }
    }

    private fun prettyName(s: String): String =
        s.trim().lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
            .joinToString(" ") { w -> w.replaceFirstChar { it.titlecase() } }

    // ---------- Enregistrement ----------

    fun suggestedDrinkBefore(d: Draft): Int {
        val base = d.vintage.trim().toIntOrNull() ?: LocalDate.now().year
        return base + Beers.gardeYears(d.style)
    }

    /** mode : "tasting", "cellar", "wish" ou "beer" (fiche seule). Renvoie l'id de la bière. */
    suspend fun save(d: Draft, mode: String): Long {
        val now = System.currentTimeMillis()
        val breweryName = d.breweryName.trim()
        val breweryId: Long? = if (breweryName.isBlank()) null else {
            val existing = dao.breweryByName(breweryName)
            if (existing != null) {
                val country = if (d.country.isNotBlank()) d.country.trim() else existing.country
                val trappist = existing.trappist || d.trappist
                if (country != existing.country || trappist != existing.trappist) {
                    dao.updateBrewery(existing.copy(country = country, trappist = trappist))
                }
                existing.id
            } else {
                dao.insertBrewery(Brewery(name = breweryName, country = d.country.trim(), trappist = d.trappist))
            }
        }
        val abv = Beers.parseAbv(d.abv)
        val old = d.beerId?.let { dao.beer(it) }
        val beerId = if (old != null) {
            dao.updateBeer(
                old.copy(
                    name = d.name.trim(),
                    breweryId = breweryId,
                    style = d.style,
                    abv = abv,
                    photoPath = d.photoPath ?: old.photoPath,
                    rebuy = d.rebuy,
                )
            )
            old.id
        } else {
            dao.insertBeer(
                Beer(
                    name = d.name.trim(),
                    breweryId = breweryId,
                    style = d.style,
                    abv = abv,
                    photoPath = d.photoPath,
                    rebuy = d.rebuy,
                    createdAt = now,
                )
            )
        }
        val code = d.barcode
        if (!code.isNullOrBlank()) dao.insertBarcode(Barcode(code, beerId, d.format))

        when (mode) {
            "tasting" -> {
                val existing = d.tastingId?.let { dao.tasting(it) }
                val tasting =
                    Tasting(
                        id = existing?.id ?: 0,
                        beerId = beerId,
                        date = existing?.date ?: now,
                        rating = d.rating.toDouble(),
                        aromas = d.aromas.sorted().joinToString(", "),
                        place = d.place,
                        service = d.service,
                        vintage = d.vintage.trim().toIntOrNull(),
                        format = d.format,
                        comment = d.comment.trim(),
                        color = d.color.ifBlank { null },
                        sweetness = d.sweetness,
                        bitterness = d.bitterness,
                        body = d.body,
                        fizz = d.fizz,
                        finish = d.finish.ifBlank { null },
                    )
                lastTastingId = if (existing != null) {
                    dao.updateTasting(tasting)
                    existing.id
                } else {
                    dao.insertTasting(tasting)
                }
            }
            "cellar" -> {
                val vintage = d.vintage.trim().toIntOrNull()
                val limit = d.drinkBefore.trim().toIntOrNull() ?: suggestedDrinkBefore(d)
                val same = dao.cellarFor(beerId).firstOrNull { it.vintage == vintage && it.format == d.format }
                if (same != null) {
                    dao.updateCellar(same.copy(quantity = same.quantity + d.quantity, drinkBefore = limit))
                } else {
                    dao.insertCellar(
                        CellarItem(
                            beerId = beerId, quantity = d.quantity, vintage = vintage,
                            format = d.format, drinkBefore = limit, addedAt = now,
                        )
                    )
                }
            }
            "wish" -> dao.insertWish(WishItem(beerId = beerId, note = d.wishNote.trim(), addedAt = now))
            else -> Unit
        }
        return beerId
    }

    // ---------- Actions rapides ----------

    suspend fun openBottle(item: CellarItem) {
        if (item.quantity <= 1) dao.deleteCellar(item) else dao.updateCellar(item.copy(quantity = item.quantity - 1))
        prepareForBeer(item.beerId, item.vintage, item.format)
        draft = draft.copy(service = "Bouteille", source = "Bouteille ouverte depuis la cave")
    }

    fun changeQuantity(item: CellarItem, delta: Int) = viewModelScope.launch {
        val q = item.quantity + delta
        if (q <= 0) dao.deleteCellar(item) else dao.updateCellar(item.copy(quantity = q))
    }

    suspend fun tastedWish(w: WishItem) {
        dao.deleteWish(w)
        prepareForBeer(w.beerId)
        draft = draft.copy(source = "Sortie de la wishlist")
    }

    fun deleteWish(w: WishItem) = viewModelScope.launch { dao.deleteWish(w) }

    fun deleteTasting(t: Tasting) = viewModelScope.launch { dao.deleteTasting(t) }

    fun setRebuy(beer: Beer, value: String) = viewModelScope.launch {
        dao.updateBeer(beer.copy(rebuy = if (beer.rebuy == value) "" else value))
    }

    fun deleteBeer(beer: Beer) = viewModelScope.launch {
        dao.deleteTastingsFor(beer.id)
        dao.deleteCellarFor(beer.id)
        dao.deleteWishesFor(beer.id)
        dao.deleteBarcodesFor(beer.id)
        dao.deleteBeer(beer)
        beer.photoPath?.let { runCatching { File(it).delete() } }
    }

    fun addAroma(label: String) = viewModelScope.launch {
        val l = label.trim()
        if (l.isNotEmpty()) dao.insertAromas(listOf(Aroma(l, "Perso", custom = true)))
    }

    // ---------- Export / sauvegarde ----------

    suspend fun exportCsv(uri: Uri) = withContext(Dispatchers.IO) {
        val beersById = dao.allBeers().associateBy { it.id }
        val breweriesById = dao.allBreweries().associateBy { it.id }
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val sb = StringBuilder()
        sb.append("date,biere,brasserie,pays,style,degre,note,aromes,lieu,service,millesime,format,a_racheter,commentaire,robe,douceur,amertume,corps,petillance,finale\n")
        for (t in dao.allTastings()) {
            val beer = beersById[t.beerId] ?: continue
            val br = beer.breweryId?.let { breweriesById[it] }
            val date = Instant.ofEpochMilli(t.date).atZone(ZoneId.systemDefault()).toLocalDate().format(fmt)
            val cells = listOf(
                date, beer.name, br?.name.orEmpty(), br?.country.orEmpty(), beer.style,
                beer.abv?.toString().orEmpty(), t.rating.toString(), t.aromas, t.place, t.service,
                t.vintage?.toString().orEmpty(), t.format, beer.rebuy, t.comment,
                t.color.orEmpty(), t.sweetness?.toString().orEmpty(), t.bitterness?.toString().orEmpty(),
                t.body?.toString().orEmpty(), t.fizz?.toString().orEmpty(), t.finish.orEmpty(),
            )
            sb.append(cells.joinToString(",") { csvCell(it) }).append('\n')
        }
        app.contentResolver.openOutputStream(uri)?.use { it.write(sb.toString().toByteArray(Charsets.UTF_8)) }
            ?: error("Impossible d'écrire le fichier")
    }

    private fun csvCell(s: String): String =
        if (s.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + s.replace("\"", "\"\"") + "\"" else s

    suspend fun backup(uri: Uri) = withContext(Dispatchers.IO) {
        val b = Backup(
            breweries = dao.allBreweries(),
            beers = dao.allBeers(),
            barcodes = dao.allBarcodes(),
            tastings = dao.allTastings(),
            cellar = dao.allCellar(),
            wishes = dao.allWishes(),
            aromas = dao.allAromas(),
            exclusions = dao.allExclusions(),
        )
        val txt = json.encodeToString(Backup.serializer(), b)
        app.contentResolver.openOutputStream(uri)?.use { it.write(txt.toByteArray(Charsets.UTF_8)) }
            ?: error("Impossible d'écrire le fichier")
    }

    suspend fun restore(uri: Uri) {
        val txt = withContext(Dispatchers.IO) {
            app.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        } ?: error("Fichier illisible")
        val b = json.decodeFromString(Backup.serializer(), txt)
        db.withTransaction {
            dao.clearTastings(); dao.clearCellar(); dao.clearWishes(); dao.clearBarcodes()
            dao.clearBeers(); dao.clearBreweries(); dao.clearAromas()
            dao.insertBreweries(b.breweries)
            dao.insertBeers(b.beers)
            dao.insertBarcodes(b.barcodes)
            dao.insertTastings(b.tastings)
            dao.insertCellarItems(b.cellar)
            dao.insertWishes(b.wishes)
            dao.replaceAromas(b.aromas)
            dao.clearExclusions()
            dao.insertExclusions(b.exclusions)
            dao.insertAromas(Beers.DEFAULT_AROMAS)
        }
    }
}

suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
}
