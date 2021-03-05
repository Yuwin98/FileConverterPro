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



}