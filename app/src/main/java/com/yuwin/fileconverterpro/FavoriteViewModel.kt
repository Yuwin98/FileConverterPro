package com.yuwin.fileconverterpro

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import com.yuwin.fileconverterpro.db.AppDatabase
import com.yuwin.fileconverterpro.db.Repository

class FavoriteViewModel(app: Application): AndroidViewModel(app) {

    private val convertedFilesDao = AppDatabase.getInstance(app).convertedFileDao()
    private val repository = Repository(convertedFilesDao)

    val readFavoriteFiles = repository.getAllFavoriteFiles().asLiveData()

}

