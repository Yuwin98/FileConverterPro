package com.yuwin.fileconverterpro

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SelectedPageInfoList(
    val items: List<SelectedPageInfo>
):Parcelable
