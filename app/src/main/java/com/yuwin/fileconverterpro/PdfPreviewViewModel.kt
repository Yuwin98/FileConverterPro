package com.yuwin.fileconverterpro

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PdfPreviewViewModel: ViewModel() {

    private val _progressValue = MutableLiveData<Int>().apply { value = 0 }
    val progressValue: LiveData<Int> get() = _progressValue

    private val _progressText = MutableLiveData<String>()
    val progressText: LiveData<String> get() =  _progressText


    init {
        _progressText.postValue("")
    }

    fun incrementProgressValue() {
        _progressValue.value?.let { a ->
            _progressValue.value = a + 1
        }
    }

    fun changeProgressText(value: Int) {
         _progressText.postValue("Opening ${progressValue.value} of $value")
    }



}