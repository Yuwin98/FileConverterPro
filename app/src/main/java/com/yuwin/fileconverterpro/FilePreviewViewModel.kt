package com.yuwin.fileconverterpro

import android.app.Application
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.yuwin.fileconverterpro.db.AppDatabase
import com.yuwin.fileconverterpro.db.ConvertedFile
import com.yuwin.fileconverterpro.db.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class FilePreviewViewModel(private val app: Application, private val id: Long): AndroidViewModel(app) {

    private var convertedDao = AppDatabase.getInstance(app).convertedFileDao()
    private var repository = Repository(convertedDao)

    private val _isFavorite = MutableLiveData<Boolean>()
    val isFavorite: LiveData<Boolean> get() = _isFavorite

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _isFavorite.postValue(repository.isFavorite(id))
        }
    }

    fun setFavorite(convertedFile: ConvertedFile, value: Boolean) {
        _isFavorite.postValue(value)
         val file = convertedFile.apply {
            isFavorite = value
        }
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateFile(file)
        }
    }

    fun deleteFile(file: ConvertedFile) {
        viewModelScope.launch(Dispatchers.IO) {
            Util.deleteFileFromStorage(file)
            repository.deleteFile(file)
        }
    }

    fun rename(convertedFile: ConvertedFile, newName: String) {
        val newFileNameWithExtension = "$newName.${convertedFile.fileType.replace(".","")}"
        val newPath = Util.getStoragePathWithExtension(Util.getExternalDir(app), newName, ".${convertedFile.fileType.replace(".","")}")
        val newUri = File(newPath).toUri()

        File(convertedFile.filePath).renameTo(File(newPath))

        val file = convertedFile.apply {
            fileName = newFileNameWithExtension
            filePath = newPath
            uri = newUri
        }
        viewModelScope.launch {
            repository.updateFile(file)
        }


    }
}