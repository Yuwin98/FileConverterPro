package com.yuwin.fileconverterpro

import android.app.Application
import androidx.lifecycle.*
import com.yuwin.fileconverterpro.repository.DataStoreRepository
import kotlinx.coroutines.launch

class MainViewModel(app: Application): AndroidViewModel(app) {

    private val repository = DataStoreRepository(app)

// Read/Write  Default  Quality Value

    val readQuality = repository.defaultQuality.asLiveData()

    fun setQuality(value: Int) {
        viewModelScope.launch {
            repository.setDefaultQuality(value)
        }
    }

    // Read/Write If review already prompted

    val readReviewPrompted = repository.reviewPrompted.asLiveData()

    fun setReviewPrompted(value: Boolean) {
        viewModelScope.launch {
            repository.setReviewPrompted(value)
        }
    }

    // Read/Write number of times app opened

    val readAppOpenedTimes = repository.openedTimes.asLiveData()

    fun incrementAppOpenedTimes() {
        viewModelScope.launch {
            repository.incrementOpenedTimes()
        }
    }

    // Read-Write default convert format

    val readDefaultFormat = repository.formatType.asLiveData()

    fun setFormatType(value: Int) {
        viewModelScope.launch {
            repository.setDefaultFormatType(value)
        }
    }


    // Read - Write Is Grid Enabled

    val readIfGridEnabled = repository.isGridEnabled.asLiveData()

    fun setIsGrid(value: Boolean) {
        viewModelScope.launch {
            repository.setIsGrid(value)
        }
    }

    // Read - Write Storage Directory

    val readCurrentStorage = repository.storageDirectory.asLiveData()

    fun setCurrentStorage(path: String) {
        viewModelScope.launch {
            repository.setStorageDirectory(path)
        }
    }


}