package fr.damien.zytho.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class OffProduct(
    val name: String,
    val brand: String,
    val abv: Double?,
    val style: String,
    val format: String,
    val country: String,
    val trappist: Boolean,
    val imageUrl: String?,
)

/** Recherche d'un code-barres dans Open Food Facts (réseau requis). */
object OpenFoodFacts {
    private const val UA = "Zytho/1.1 (Android; usage perso)"

    suspend fun lookup(code: String): OffProduct? = withContext(Dispatchers.IO) {
        try {
            val url = URL(
                "https://world.openfoodfacts.org/api/v2/product/$code.json" +
                    "?fields=product_name,product_name_fr,generic_name,generic_name_fr,brands,quantity," +
                    "categories_tags,labels_tags,image_front_url,nutriments"
            )
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 7000
            conn.readTimeout = 7000
            conn.setRequestProperty("User-Agent", UA)
            try {
                if (conn.responseCode != 200) {
                    null
                } else {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    parse(body)
                }
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Télécharge la photo du produit dans [dest]. Renvoie true si réussi. */
    suspend fun downloadImage(url: String, dest: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 7000
            conn.readTimeout = 10000
            conn.setRequestProperty("User-Agent", UA)
            try {
                if (conn.responseCode != 200) {
                    false
                } else {
                    conn.inputStream.use { input -> dest.outputStream().use { input.copyTo(it) } }
                    dest.length() > 0
                }
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun tags(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).map { arr.optString(it, "") }.filter { it.isNotEmpty() }
    }

    private fun parse(body: String): OffProduct? {
        val json = JSONObject(body)
        if (json.optInt("status", 0) != 1) return null
        val p = json.optJSONObject("product") ?: return null
        val name = p.optString("product_name_fr", "").ifBlank { p.optString("product_name", "") }.trim()
        val generic = p.optString("generic_name_fr", "").ifBlank { p.optString("generic_name", "") }.trim()
        val brand = p.optString("brands", "").split(",").firstOrNull()?.trim().orEmpty()
        val categories = tags(p.optJSONArray("categories_tags"))
        val labels = tags(p.optJSONArray("labels_tags"))

        val n = p.optJSONObject("nutriments")
        var abv: Double? = null
        if (n != null) {
            val v = when {
                n.has("alcohol") -> n.optDouble("alcohol")
                n.has("alcohol_100g") -> n.optDouble("alcohol_100g")
                else -> Double.NaN
            }
            if (!v.isNaN() && v > 0) abv = v
        }
        if (name.isBlank() && brand.isBlank()) return null

        val catText = categories.joinToString(" ") { it.substringAfter(':').replace('-', ' ') }
        val style = detectStyle(generic, name, catText)
        val country = when {
            categories.any { it.contains("belgian") } -> "Belgique"
            categories.any { it.contains("french") } -> "France"
            categories.any { it.contains("luxembourg") } -> "Luxembourg"
            categories.any { it.contains("german") } -> "Allemagne"
            categories.any { it.contains("dutch") } -> "Pays-Bas"
            categories.any { it.contains("british") || it.contains("english") } -> "Royaume-Uni"
            else -> ""
        }
        val trappist = labels.any { it.contains("trappist") } ||
            (generic + " " + name).lowercase().contains("trappist")
        val format = parseFormat(p.optString("quantity", ""))
        val image = p.optString("image_front_url", "").ifBlank { null }
        return OffProduct(name, brand, abv, style, format, country, trappist, image)
    }

    fun parseFormat(q: String): String {
        val m = Regex("""(\d+(?:[.,]\d+)?)\s*(cl|ml|l)\b""", RegexOption.IGNORE_CASE).find(q) ?: return ""
        val v = m.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return ""
        val cl = when (m.groupValues[2].lowercase()) {
            "ml" -> v / 10
            "l" -> v * 100
            else -> v
        }
        return when {
            cl < 29 -> "25 cl"
            cl < 35.5 -> "33 cl"
            cl < 50 -> "37,5 cl"
            cl < 100 -> "75 cl"
            else -> ""
        }
    }

    /** Devine le style à partir des textes, du plus fiable au moins fiable. */
    fun detectStyle(vararg texts: String): String {
        for (t in texts) {
            val s = t.lowercase()
            if (s.isBlank()) continue
            val found = when {
                s.contains("quadru") || Regex("\\bquad\\b").containsMatchIn(s) -> "Quadrupel"
                s.contains("tripel") || s.contains("triple") || s.contains("trippel") -> "Tripel"
                s.contains("dubbel") || s.contains("double") -> "Dubbel"
                s.contains("gueuze") || s.contains("geuze") -> "Gueuze"
                s.contains("kriek") -> "Kriek"
                s.contains("lambic") || s.contains("lambiek") -> "Lambic"
                s.contains("saison") -> "Saison"
                s.contains("witbier") || s.contains("blanche") || s.contains("wheat") || s.contains("weiss") -> "Witbier"
                s.contains("barley wine") || s.contains("barleywine") -> "Barley wine"
                s.contains("scotch") -> "Scotch"
                s.contains("stout") || s.contains("porter") -> "Stout"
                Regex("\\bipa\\b").containsMatchIn(s) || s.contains("india pale") -> "IPA"
                s.contains("strong golden") || s.contains("blonde forte") || s.contains("golden strong") -> "Strong Golden Ale"
                s.contains("ambrée") || s.contains("ambree") || s.contains("amber") -> "Ambrée"
                s.contains("brune") || s.contains("foncée") || s.contains("bruin") || s.contains("brown") || s.contains("dark ale") -> "Brune"
                s.contains("blonde") || s.contains("golden") || s.contains("pale ale") -> "Blonde"
                else -> null
            }
            if (found != null) return found
        }
        return ""
    }
}
