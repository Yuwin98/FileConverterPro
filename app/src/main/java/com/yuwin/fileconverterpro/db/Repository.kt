package com.yuwin.fileconverterpro.db

import androidx.lifecycle.LiveData

class Repository(private val database: AppDatabase) {

    suspend fun getAllFiles(): List<ConvertedFile> {
        return database.convertedFileDao().getAllFiles()
    }

}