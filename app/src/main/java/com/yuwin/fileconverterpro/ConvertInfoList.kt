package com.yuwin.fileconverterpro

import android.os.Parcelable
import com.yuwin.fileconverterpro.ConvertInfo
import kotlinx.parcelize.Parcelize

@Parcelize
data class ConvertInfoList(
        val items: List<ConvertInfo>
):Parcelable
