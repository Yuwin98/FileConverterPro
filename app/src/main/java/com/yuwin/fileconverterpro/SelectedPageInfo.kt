package com.yuwin.fileconverterpro

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SelectedPageInfo(
    val pdfIndex: Int,
    val selectedPages: List<Int>
): Parcelable
