package com.yuwin.fileconverterpro

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.yuwin.fileconverterpro.db.AppDatabase
import com.yuwin.fileconverterpro.db.ConvertedFile
import com.yuwin.fileconverterpro.db.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DirectoryPreviewViewModel(val app: Application) : AndroidViewModel(app) {

    private val convertedDao = AppDatabase.getInstance(app).convertedFileDao()
    private val repository = Repository(convertedDao)


    val allDirectoryFiles = repository.getAllFilesInDirectory().asLiveData()
    val allFiles = repository.getAllFiles().asLiveData()


    fun clearDirectory(files: List<ConvertedFile>) {
        files.forEach { file ->
            deleteSelectedFiles(file)
        }

    }

    private fun deleteSelectedFiles(file: ConvertedFile) {
        viewModelScope.launch(Dispatchers.IO) {
            Util.deleteFileFromStorage(file, app.applicationContext)
        }
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteFile(file)
        }
    }

}