package com.yuwin.fileconverterpro

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import com.yuwin.fileconverterpro.db.AppDatabase
import com.yuwin.fileconverterpro.db.Repository

class FileListViewModel(app: Application): AndroidViewModel(app) {

    private val database: AppDatabase = AppDatabase.getInstance(app.applicationContext)
    private val repository: Repository = Repository(database.convertedFileDao())


    val readFiles = repository.getAllFiles().asLiveData()

}