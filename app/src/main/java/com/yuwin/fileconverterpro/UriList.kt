package com.yuwin.fileconverterpro

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class UriList(
    val items: List<Uri>
): Parcelable