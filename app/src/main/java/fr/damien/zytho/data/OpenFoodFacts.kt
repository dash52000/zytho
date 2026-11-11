package fr.damien.zytho.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class OffProduct(val name: String, val brand: String, val abv: Double?)

/** Recherche d'un code-barres dans Open Food Facts (réseau requis). */
object OpenFoodFacts {
    suspend fun lookup(code: String): OffProduct? = withContext(Dispatchers.IO) {
        try {
            val url = URL(
                "https://world.openfoodfacts.org/api/v2/product/$code.json" +
                    "?fields=product_name,product_name_fr,brands,nutriments"
            )
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 7000
            conn.readTimeout = 7000
            conn.setRequestProperty("User-Agent", "Zytho/1.0 (Android; usage perso)")
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

    private fun parse(body: String): OffProduct? {
        val json = JSONObject(body)
        if (json.optInt("status", 0) != 1) return null
        val p = json.optJSONObject("product") ?: return null
        val name = p.optString("product_name_fr", "").ifBlank { p.optString("product_name", "") }.trim()
        val brand = p.optString("brands", "").split(",").firstOrNull()?.trim().orEmpty()
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
        return OffProduct(name, brand, abv)
    }
}
