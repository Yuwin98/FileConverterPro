package com.yuwin.fileconverterpro

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.yuwin.fileconverterpro.db.AppDatabase
import com.yuwin.fileconverterpro.db.ConvertedFile
import com.yuwin.fileconverterpro.db.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

    fun deleteFile(file: ConvertedFile, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            Util.deleteFileFromStorage(file, context)
            repository.deleteFile(file)
        }
    }


}