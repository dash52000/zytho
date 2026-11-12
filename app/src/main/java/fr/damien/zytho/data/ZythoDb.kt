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
        CellarItem::class, WishItem::class, Aroma::class, CatalogExclusion::class, Profile::class,
    ],
    version = 4,
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

        /** v3 -> v4 : profils (dégustations, wishlist et exclusions par profil). */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `profile` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)"
                )
                db.execSQL("INSERT OR IGNORE INTO `profile` (`id`, `name`, `createdAt`) VALUES (1, 'Moi', 0)")
                db.execSQL("ALTER TABLE `tasting` ADD COLUMN `profileId` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `wish` ADD COLUMN `profileId` INTEGER NOT NULL DEFAULT 1")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `catalog_exclusion_new` (`name` TEXT NOT NULL, `addedAt` INTEGER NOT NULL, `profileId` INTEGER NOT NULL DEFAULT 1, PRIMARY KEY(`profileId`, `name`))"
                )
                db.execSQL("INSERT INTO `catalog_exclusion_new` (`name`, `addedAt`, `profileId`) SELECT `name`, `addedAt`, 1 FROM `catalog_exclusion`")
                db.execSQL("DROP TABLE `catalog_exclusion`")
                db.execSQL("ALTER TABLE `catalog_exclusion_new` RENAME TO `catalog_exclusion`")
            }
        }

        fun get(context: Context): ZythoDb =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ZythoDb::class.java,
                    "zytho.db",
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build().also { instance = it }
            }
    }
}
