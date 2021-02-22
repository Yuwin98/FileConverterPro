package com.yuwin.fileconverterpro.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "converted_files")
class ConvertedFile(
        @PrimaryKey
        val id: Long,
        var fileName: String,
        val fileSize: String,
        var filePath: String,
        val fileType: String,
        val date: String

)
