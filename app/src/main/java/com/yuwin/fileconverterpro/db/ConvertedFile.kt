package com.yuwin.fileconverterpro.db

import android.net.Uri
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
@Entity
data class ConvertedFile(

        @PrimaryKey(autoGenerate = true)
        @ColumnInfo(index = true, name = "Id")
        val id: Long,

        @ColumnInfo(name = "fileName")
        var fileName: String,

        @ColumnInfo(name = "fileSize")
        val fileSize: String,

        @ColumnInfo(name = "filePath")
        var filePath: String,

        @ColumnInfo(name = "fileType")
        val fileType: String,

        @ColumnInfo(name = "Uri")
        var uri: Uri,

        @ColumnInfo(name = "thumbnailUri")
        val thumbnailUri: Uri?,

        @ColumnInfo(name = "favorite")
        var isFavorite: Boolean,

        @ColumnInfo(name = "Date")
        val date: Date

): Parcelable
