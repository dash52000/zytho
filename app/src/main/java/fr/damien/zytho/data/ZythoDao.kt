package fr.damien.zytho.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ZythoDao {
    // Flux observés par l'interface
    @Query("SELECT * FROM brewery ORDER BY name COLLATE NOCASE")
    fun breweries(): Flow<List<Brewery>>

    @Query("SELECT * FROM beer ORDER BY name COLLATE NOCASE")
    fun beers(): Flow<List<Beer>>

    @Query("SELECT * FROM tasting WHERE profileId = :profileId ORDER BY date DESC")
    fun tastings(profileId: Long): Flow<List<Tasting>>

    @Query("SELECT * FROM cellar ORDER BY addedAt DESC")
    fun cellar(): Flow<List<CellarItem>>

    @Query("SELECT * FROM wish WHERE profileId = :profileId ORDER BY addedAt DESC")
    fun wishes(profileId: Long): Flow<List<WishItem>>

    @Query("SELECT * FROM profile ORDER BY id")
    fun profiles(): Flow<List<Profile>>

    @Query("SELECT * FROM barcode")
    fun barcodes(): Flow<List<Barcode>>

    @Query("SELECT * FROM aroma ORDER BY family, label")
    fun aromas(): Flow<List<Aroma>>

    @Query("SELECT * FROM catalog_exclusion WHERE profileId = :profileId")
    fun exclusions(profileId: Long): Flow<List<CatalogExclusion>>

    // Lectures ponctuelles
    @Query("SELECT * FROM brewery") suspend fun allBreweries(): List<Brewery>
    @Query("SELECT * FROM beer") suspend fun allBeers(): List<Beer>
    @Query("SELECT * FROM barcode") suspend fun allBarcodes(): List<Barcode>
    @Query("SELECT * FROM tasting ORDER BY date DESC") suspend fun allTastings(): List<Tasting>
    @Query("SELECT * FROM cellar") suspend fun allCellar(): List<CellarItem>
    @Query("SELECT * FROM wish") suspend fun allWishes(): List<WishItem>
    @Query("SELECT * FROM aroma") suspend fun allAromas(): List<Aroma>
    @Query("SELECT * FROM catalog_exclusion") suspend fun allExclusions(): List<CatalogExclusion>
    @Query("SELECT * FROM profile") suspend fun allProfiles(): List<Profile>

    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertProfileIfMissing(p: Profile)
    @Insert suspend fun insertProfile(p: Profile): Long
    @Update suspend fun updateProfile(p: Profile)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertProfiles(l: List<Profile>)
    @Query("DELETE FROM profile") suspend fun clearProfiles()

    @Query("SELECT * FROM barcode WHERE code = :code LIMIT 1")
    suspend fun barcode(code: String): Barcode?

    @Query("SELECT * FROM beer WHERE id = :id LIMIT 1")
    suspend fun beer(id: Long): Beer?

    @Query("SELECT * FROM brewery WHERE id = :id LIMIT 1")
    suspend fun brewery(id: Long): Brewery?

    @Query("SELECT * FROM brewery WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun breweryByName(name: String): Brewery?

    @Query("SELECT * FROM cellar WHERE beerId = :beerId")
    suspend fun cellarFor(beerId: Long): List<CellarItem>

    // Écritures
    @Insert suspend fun insertBrewery(b: Brewery): Long
    @Update suspend fun updateBrewery(b: Brewery)

    @Insert suspend fun insertBeer(b: Beer): Long
    @Update suspend fun updateBeer(b: Beer)
    @Delete suspend fun deleteBeer(b: Beer)

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertBarcode(b: Barcode)

    @Insert suspend fun insertTasting(t: Tasting): Long
    @Update suspend fun updateTasting(t: Tasting)
    @Delete suspend fun deleteTasting(t: Tasting)

    @Query("SELECT * FROM tasting WHERE id = :id LIMIT 1")
    suspend fun tasting(id: Long): Tasting?

    @Insert suspend fun insertCellar(c: CellarItem): Long
    @Update suspend fun updateCellar(c: CellarItem)
    @Delete suspend fun deleteCellar(c: CellarItem)

    @Insert suspend fun insertWish(w: WishItem): Long
    @Delete suspend fun deleteWish(w: WishItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertExclusion(e: CatalogExclusion)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertExclusions(l: List<CatalogExclusion>)
    @Query("DELETE FROM catalog_exclusion") suspend fun clearExclusions()

    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertAromas(list: List<Aroma>)

    @Query("DELETE FROM tasting WHERE beerId = :beerId") suspend fun deleteTastingsFor(beerId: Long)
    @Query("DELETE FROM cellar WHERE beerId = :beerId") suspend fun deleteCellarFor(beerId: Long)
    @Query("DELETE FROM wish WHERE beerId = :beerId") suspend fun deleteWishesFor(beerId: Long)
    @Query("DELETE FROM barcode WHERE beerId = :beerId") suspend fun deleteBarcodesFor(beerId: Long)

    // Restauration
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertBreweries(l: List<Brewery>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertBeers(l: List<Beer>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertBarcodes(l: List<Barcode>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertTastings(l: List<Tasting>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertCellarItems(l: List<CellarItem>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertWishes(l: List<WishItem>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun replaceAromas(l: List<Aroma>)

    @Query("DELETE FROM brewery") suspend fun clearBreweries()
    @Query("DELETE FROM beer") suspend fun clearBeers()
    @Query("DELETE FROM barcode") suspend fun clearBarcodes()
    @Query("DELETE FROM tasting") suspend fun clearTastings()
    @Query("DELETE FROM cellar") suspend fun clearCellar()
    @Query("DELETE FROM wish") suspend fun clearWishes()
    @Query("DELETE FROM aroma") suspend fun clearAromas()
}
