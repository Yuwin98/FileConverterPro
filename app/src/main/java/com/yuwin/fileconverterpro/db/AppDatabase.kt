package com.yuwin.fileconverterpro.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.yuwin.fileconverterpro.ConvertInfo

@Database(entities = [ConvertInfo::class], version = 1, exportSchema = false)
abstract class AppDatabase: RoomDatabase() {

    abstract fun convertedFileDao(): ConvertedFileDao

    companion object {

        @Volatile private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, "file_database")
                    .build()
        }
    }

}