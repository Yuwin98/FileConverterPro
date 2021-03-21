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
    var convertInto: String
    ): Parcelable

