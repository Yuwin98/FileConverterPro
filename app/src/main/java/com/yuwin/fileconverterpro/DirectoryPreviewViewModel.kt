package com.yuwin.fileconverterpro

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import com.yuwin.fileconverterpro.db.AppDatabase
import com.yuwin.fileconverterpro.db.ConvertedFile
import com.yuwin.fileconverterpro.db.Repository
import kotlinx.coroutines.flow.Flow

class DirectoryPreviewViewModel(app:Application): AndroidViewModel(app) {

    private val convertedDao = AppDatabase.getInstance(app).convertedFileDao()
    private val repository = Repository(convertedDao)


    val allDirectoryFiles = repository.getAllFilesInDirectory().asLiveData()

}