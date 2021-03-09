package com.yuwin.fileconverterpro

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ConvertInfoList(
        val items: List<ConvertInfo>
):Parcelable
