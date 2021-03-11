package com.yuwin.fileconverterpro

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel


class ConvertViewModel: ViewModel() {


    private val _allConvert = MutableLiveData<Boolean>()
    val allConvert: LiveData<Boolean> get() = _allConvert

    private val _defaultPosition = MutableLiveData<Int>()
    val defaultPosition: LiveData<Int> get() = _defaultPosition

    private val _qualityValue = MutableLiveData<String>()
    val qualityValue: LiveData<String> get() = _qualityValue

    private val _isPDFConversion = MutableLiveData<Boolean>()
    val isPdfConversion: LiveData<Boolean> get() = _isPDFConversion

    init {
        _allConvert.postValue(false)
        _qualityValue.postValue("100")

    }


    fun setOnConvertAllCheckChanged(value: Boolean) {
        _allConvert.postValue(value)
    }

    fun setDefaultSpinnerPosition(value: Int) {
        _defaultPosition.postValue(value)
    }

    fun setQualityValue(value: String) {
        _qualityValue.postValue(value)
    }

    fun setIsPdfConversion(value: Boolean) {
        _isPDFConversion.postValue(value)
    }








}