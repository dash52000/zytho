package fr.damien.zytho.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Brewery::class, Beer::class, Barcode::class, Tasting::class,
        CellarItem::class, WishItem::class, Aroma::class, CatalogExclusion::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class ZythoDb : RoomDatabase() {
    abstract fun dao(): ZythoDao

    companion object {
        @Volatile
        private var instance: ZythoDb? = null

        /** v1 -> v2 : champs de la dégustation guidée. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasting ADD COLUMN color TEXT")
                db.execSQL("ALTER TABLE tasting ADD COLUMN sweetness INTEGER")
                db.execSQL("ALTER TABLE tasting ADD COLUMN bitterness INTEGER")
                db.execSQL("ALTER TABLE tasting ADD COLUMN body INTEGER")
                db.execSQL("ALTER TABLE tasting ADD COLUMN fizz INTEGER")
                db.execSQL("ALTER TABLE tasting ADD COLUMN finish TEXT")
            }
        }

        /** v2 -> v3 : bières du catalogue écartées par « Pas pour moi ». */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `catalog_exclusion` (`name` TEXT NOT NULL, `addedAt` INTEGER NOT NULL, PRIMARY KEY(`name`))"
                )
            }
        }

        fun get(context: Context): ZythoDb =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ZythoDb::class.java,
                    "zytho.db",
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
            }
    }
}
