package com.yuwin.fileconverterpro.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ConvertedFileDao {

    @Query("Select * from ConvertedFile Order by Date DESC")
    fun getAllFiles(): Flow<List<ConvertedFile>>

    @Query("Select * from ConvertedFile where filePath=:path")
    fun getFile(path: String): ConvertedFile

    @Query("Select * from ConvertedFile where filePath like :path")
    fun getAllDirectoryFilesWithFilePath(path: String): Flow<List<ConvertedFile>>

    @Query("Delete from ConvertedFile where filePath like :path")
    fun deleteFilesAndFoldersInPath(path: String)

    @Query("Select * from ConvertedFile where favorite=1")
    fun getAllFavoriteFiles(): Flow<List<ConvertedFile>>

    @Query("Select * from ConvertedFile where inDirectory=1")
    fun getAllFilesInDirectories(): Flow<List<ConvertedFile>>

    @Query("Select * from ConvertedFile where inDirectory=0 Order By Date DESC")
    fun getAllFilesInRoot(): Flow<List<ConvertedFile>>

    @Query("Select * from ConvertedFile Order By fileName ")
    fun getAllFilesByName() : Flow<List<ConvertedFile>>

    @Query("Select * from ConvertedFile Order By fileType ")
    fun getAllFilesByType() : Flow<List<ConvertedFile>>

    @Query("Select * from ConvertedFile Order By Date DESC ")
    fun getAllFilesByDateCreated() : Flow<List<ConvertedFile>>

    @Query("Select * from ConvertedFile Order By fileSizeValue DESC ")
    fun getAllFilesBySize() : Flow<List<ConvertedFile>>

    @Query("Select * from ConvertedFile where fileType in (:types) Order by Date DESC ")
    fun filterAllFilesByType(types: List<String>) : Flow<List<ConvertedFile>>

    @Query("SELECT * FROM ConvertedFile WHERE filePath LIKE :filePath AND fileName LIKE :query")
    fun searchDatabaseInDirectory(query: String, filePath: String): Flow<List<ConvertedFile>>

    @Query("SELECT * FROM ConvertedFile WHERE inDirectory= 0 AND fileName Like :query Order By Date DESC")
    fun searchDatabaseInRoot(query: String): Flow<List<ConvertedFile>>

    @Query("SELECT * FROM ConvertedFile WHERE filePath LIKE :filePath")
    fun getPublicUrisInFolder(filePath: String): Flow<List<ConvertedFile>>

    @Insert
    suspend fun insertFile(file: ConvertedFile)

    @Delete
    suspend fun deleteFile(file: ConvertedFile)

    @Update
    suspend fun updateFile(file: ConvertedFile)

    @Query("SELECT COUNT(*) FROM ConvertedFile")
    fun count(): Int

    @Query("Select fileSize from ConvertedFile where filePath=:path")
    fun directoryFileCount(path: String): String

    @Query("Select favorite from ConvertedFile Where Id=:id ")
    fun isFavorite(id: Long): Boolean

    @Query("Select isDirectory from ConvertedFile where Id=:id")
    fun isDirectory(id: Long): Boolean

    @Query("Delete from convertedFile")
    suspend fun deleteAll()



}