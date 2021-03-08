package com.yuwin.fileconverterpro

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.yuwin.fileconverterpro.db.AppDatabase
import com.yuwin.fileconverterpro.db.ConvertedFile
import com.yuwin.fileconverterpro.db.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class DirectoryPreviewViewModel(app:Application): AndroidViewModel(app) {

    private val convertedDao = AppDatabase.getInstance(app).convertedFileDao()
    private val repository = Repository(convertedDao)


    val allDirectoryFiles = repository.getAllFilesInDirectory().asLiveData()

    fun clearDirectory(files: List<ConvertedFile>) {
        files.forEach { file ->
            viewModelScope.launch {
                deleteSelectedFiles(file)
            }
        }

    }

    suspend fun deleteSelectedFiles(file: ConvertedFile) {
        Util.deleteFileFromStorage(file)
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteFile(file)
        }
    }

}