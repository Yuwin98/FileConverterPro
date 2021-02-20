package com.yuwin.fileconverterpro.db

import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData

class Repository(private val database: AppDatabase) {

    suspend fun getAllFiles(): LiveData<List<ConvertedFile>> {
        return database.convertedFileDao().getAllFiles()
    }

}