package com.yuwin.fileconverterpro

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ConvertProgressViewModelFactory(private val application: Application,
                                      val data: List<ConvertInfo>,
                                      private val quality: Int,
                                      private val pdfQuality: Int,
                                      private val convertInto: String,
                                      private val pageInfoList: SelectedPageInfoList?): ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(ConvertProgressViewModel::class.java)) {
            return ConvertProgressViewModel(application, data, quality, pdfQuality,convertInto, pageInfoList) as T
        }
        throw IllegalArgumentException("Unable to construct view model")
    }
}