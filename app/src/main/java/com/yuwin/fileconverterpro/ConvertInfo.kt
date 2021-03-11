package com.yuwin.fileconverterpro

import android.net.Uri
import android.os.Parcelable
import android.util.Log
import androidx.databinding.*
import kotlinx.parcelize.Parcelize

@Parcelize
data class ConvertInfo
    (
    val uri: Uri,
    val fileName: String,
    val fileSize: String,
    val filePath: String,
    val fileType: String,
    var convertAll: Boolean?,
    var isPdfConversion: Boolean?,
    var specificConvertFormat: Int?,
    var defaultConvertFormat: Int?,
    ): Parcelable, BaseObservable() {


        var specificFormat: Int?
        @Bindable get() = specificConvertFormat
        set(value) {
            specificConvertFormat = value
            Log.d("format", specificConvertFormat.toString())
            notifyPropertyChanged(BR.specificFormat)
        }


    }


