package com.yuwin.fileconverterpro.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.yuwin.fileconverterpro.ConvertInfo

@Dao
interface ConvertedFileDao {

    @Query("Select * from converted_files")
    suspend fun getAllFiles(): LiveData<List<ConvertedFile>>

    @Insert
    suspend fun insertFile(file: ConvertInfo)

    @Delete
    suspend fun deleteFile(file: ConvertInfo)



}