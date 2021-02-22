package com.yuwin.fileconverterpro

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class ConvertProgressViewModel(application: Application, data: List<ConvertInfo>): AndroidViewModel(application) {

    private val _completePercentage = MutableLiveData<Int>()
    val completePercentage: LiveData<Int> get() = _completePercentage





    fun progressToString(): String {
        return  completePercentage.value.toString()
    }

}