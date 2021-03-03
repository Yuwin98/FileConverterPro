package com.yuwin.fileconverterpro

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class FilePreviewViewModelFactory(private val app: Application, private val id: Long): ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(FilePreviewViewModel::class.java)) {
            return FilePreviewViewModel(app, id) as T
        }
        throw IllegalArgumentException("Unable to construct view model")
    }
}