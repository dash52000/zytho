package fr.damien.zytho.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        Brewery::class, Beer::class, Barcode::class, Tasting::class,
        CellarItem::class, WishItem::class, Aroma::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class ZythoDb : RoomDatabase() {
    abstract fun dao(): ZythoDao

    companion object {
        @Volatile
        private var instance: ZythoDb? = null

        fun get(context: Context): ZythoDb =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ZythoDb::class.java,
                    "zytho.db",
                ).build().also { instance = it }
            }
    }
}
