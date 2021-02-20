package com.yuwin.fileconverterpro.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "converted_files")
data class ConvertedFile(
        @PrimaryKey
        val id: Long,
        val fileName: String,
        val fileSize: String,
        val filePath: String,
        val fileType: String,
        val date: String

)
