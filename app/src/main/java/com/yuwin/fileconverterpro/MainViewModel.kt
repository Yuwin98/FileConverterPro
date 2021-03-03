package com.yuwin.fileconverterpro

import android.app.Application
import androidx.lifecycle.*
import com.yuwin.fileconverterpro.misc.UiMode
import com.yuwin.fileconverterpro.repository.DataStoreRepository
import com.yuwin.fileconverterpro.repository.appDataStore
import kotlinx.coroutines.launch

class MainViewModel(app: Application): AndroidViewModel(app) {

    private val repository = DataStoreRepository(app)

    private val _darkMode = repository.uiMode.asLiveData()
    val darkMode: LiveData<Int> get() = _darkMode

    fun setDarkMode(mode: UiMode) {
        viewModelScope.launch {
            repository.setUiMode(mode)
        }
    }



}