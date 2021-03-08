package com.yuwin.fileconverterpro

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ConvertProgressViewModelFactory(private val application: Application, val data: List<ConvertInfo>, private val quality: Int, private val pdfQuality: Int): ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(ConvertProgressViewModel::class.java)) {
            Log.d("ConvertedFilesFragment", "Convert Progress ViewModel Before Creation")
            return ConvertProgressViewModel(application, data, quality, pdfQuality) as T
        }
        throw IllegalArgumentException("Unable to construct view model")
    }
}