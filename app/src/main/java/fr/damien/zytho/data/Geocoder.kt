package fr.damien.zytho.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class Place(val label: String, val lat: Double, val lon: Double)

/** Recherche de lieux via Nominatim (OpenStreetMap). Réseau requis. */
object Geocoder {
    suspend fun search(query: String): List<Place> = withContext(Dispatchers.IO) {
        try {
            val q = URLEncoder.encode(query.trim(), "UTF-8")
            val url = URL("https://nominatim.openstreetmap.org/search?q=$q&format=json&limit=6&accept-language=fr")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("User-Agent", "Zytho/1.2 (application Android personnelle)")
            try {
                if (conn.responseCode != 200) {
                    emptyList()
                } else {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val arr = JSONArray(body)
                    (0 until arr.length()).mapNotNull { i ->
                        val o = arr.optJSONObject(i) ?: return@mapNotNull null
                        val lat = o.optString("lat").toDoubleOrNull() ?: return@mapNotNull null
                        val lon = o.optString("lon").toDoubleOrNull() ?: return@mapNotNull null
                        Place(o.optString("display_name", query), lat, lon)
                    }
                }
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
