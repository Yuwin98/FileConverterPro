package com.yuwin.fileconverterpro.db

import androidx.lifecycle.LiveData
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ConvertedFileDao {

    @Query("Select * from ConvertedFile Order by Date DESC")
    fun getAllFiles(): Flow<List<ConvertedFile>>

    @Insert
    suspend fun insertFile(file: ConvertedFile)

    @Delete
    suspend fun deleteFile(file: ConvertedFile)

    @Update
    suspend fun updateFile(file: ConvertedFile)

    @Query("SELECT COUNT(*) FROM ConvertedFile")
    fun count(): Int

    @Query("Select favorite from ConvertedFile Where Id=:id ")
    fun isFavorite(id: Long): Boolean

    @Query("Delete from convertedFile")
    suspend fun deleteAll()



}