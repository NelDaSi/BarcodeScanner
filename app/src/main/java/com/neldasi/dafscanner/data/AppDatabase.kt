package com.neldasi.dafscanner.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ScannedPart::class, ConversionRecord::class, SearchItem::class], version = 4, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scanDao(): ScanDao
    abstract fun conversionDao(): ConversionDao
    abstract fun searchItemDao(): SearchItemDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "scan_database",
                )
                    // NOTE: the next time `version` above changes, add a matching Migration
                    // via .addMigrations(...) here. Without one, Room will crash on open
                    // instead of silently deleting scan history, verification lists, and
                    // conversion history — that crash is the signal to write the migration
                    // before releasing, not a bug to "fix" by re-adding a destructive fallback.
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
