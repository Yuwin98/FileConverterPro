package com.yuwin.fileconverterpro

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore

class ImagePreviewViewModelFactory(private val app: Application, private val id: Long): ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(ImagePreviewViewModel::class.java)) {
            return ImagePreviewViewModel(app, id) as T
        }
        throw IllegalArgumentException("Unable to construct view model")
    }
}