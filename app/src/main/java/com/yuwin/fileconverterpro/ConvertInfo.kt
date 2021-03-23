package com.yuwin.fileconverterpro

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ConvertInfo
    (
    val uri: Uri,
    val fileName: String,
    val fileSize: String,
    val filePath: String,
    val fileType: String,
    val selectedPagesText: String?,
    val isSelectedPagesOperation: Boolean,
    var convertInto: String
    ): Parcelable

