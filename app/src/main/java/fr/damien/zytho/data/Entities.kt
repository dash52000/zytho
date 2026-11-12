package fr.damien.zytho.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "brewery")
data class Brewery(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val country: String = "",
    val region: String = "",
    val trappist: Boolean = false,
    val lat: Double? = null,
    val lon: Double? = null,
)

@Serializable
@Entity(tableName = "beer")
data class Beer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val breweryId: Long? = null,
    val style: String = "",
    val abv: Double? = null,
    val photoPath: String? = null,
    /** "Oui", "Peut-être", "Non" ou vide */
    val rebuy: String = "",
    val createdAt: Long = 0,
)

@Serializable
@Entity(tableName = "barcode")
data class Barcode(
    @PrimaryKey val code: String,
    val beerId: Long,
    val format: String = "",
)

@Serializable
@Entity(tableName = "tasting")
data class Tasting(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val beerId: Long,
    val date: Long,
    /** Note sur 10, par pas de 0,5 */
    val rating: Double,
    /** Arômes séparés par des virgules */
    val aromas: String = "",
    val place: String = "",
    val service: String = "",
    val vintage: Int? = null,
    val format: String = "",
    val comment: String = "",
    // Dégustation guidée (v2), tous facultatifs. Profils de 0 à 4.
    val color: String? = null,
    val sweetness: Int? = null,
    val bitterness: Int? = null,
    val body: Int? = null,
    val fizz: Int? = null,
    val finish: String? = null,
)

@Serializable
@Entity(tableName = "cellar")
data class CellarItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val beerId: Long,
    val quantity: Int = 1,
    val vintage: Int? = null,
    val format: String = "",
    /** Année limite conseillée */
    val drinkBefore: Int? = null,
    val addedAt: Long = 0,
)

@Serializable
@Entity(tableName = "wish")
data class WishItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val beerId: Long,
    val note: String = "",
    val addedAt: Long = 0,
)

@Serializable
@Entity(tableName = "aroma")
data class Aroma(
    @PrimaryKey val label: String,
    val family: String,
    val custom: Boolean = false,
)

@Serializable
data class Backup(
    val version: Int = 1,
    val breweries: List<Brewery> = emptyList(),
    val beers: List<Beer> = emptyList(),
    val barcodes: List<Barcode> = emptyList(),
    val tastings: List<Tasting> = emptyList(),
    val cellar: List<CellarItem> = emptyList(),
    val wishes: List<WishItem> = emptyList(),
    val aromas: List<Aroma> = emptyList(),
)
