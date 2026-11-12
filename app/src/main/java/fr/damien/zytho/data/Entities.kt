package fr.damien.zytho.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** Un profil de dégustateur. Le profil 1 (« Moi ») existe toujours. */
@Serializable
@Entity(tableName = "profile")
data class Profile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = 0,
)

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
    @ColumnInfo(defaultValue = "1") val profileId: Long = 1,
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
    @ColumnInfo(defaultValue = "1") val profileId: Long = 1,
)

@Serializable
@Entity(tableName = "aroma")
data class Aroma(
    @PrimaryKey val label: String,
    val family: String,
    val custom: Boolean = false,
)

/** Bière du catalogue qu'Anselme ne doit plus proposer (« Pas pour moi »). */
@Serializable
@Entity(tableName = "catalog_exclusion", primaryKeys = ["profileId", "name"])
data class CatalogExclusion(
    /** Clé normalisée du nom (Catalog.normalize) */
    val name: String,
    val addedAt: Long = 0,
    @ColumnInfo(defaultValue = "1") val profileId: Long = 1,
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
    val exclusions: List<CatalogExclusion> = emptyList(),
    val profiles: List<Profile> = emptyList(),
)
