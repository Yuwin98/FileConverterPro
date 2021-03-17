package com.yuwin.fileconverterpro.db

import android.util.Log
import androidx.lifecycle.LiveData
import kotlinx.coroutines.flow.Flow

class Repository(private val convertedFileDao: ConvertedFileDao) {

    fun getAllFiles(): Flow<List<ConvertedFile>> {
        return convertedFileDao.getAllFiles()
    }

    fun getAllDirectoryFilesWithFilePath(path: String): Flow<List<ConvertedFile>> {
        return convertedFileDao.getAllDirectoryFilesWithFilePath("$path%")
    }

    fun getAllFilesInDirectory(): Flow<List<ConvertedFile>> {
        return convertedFileDao.getAllFilesInDirectories()
    }

    fun getAllFilesInRoot(): Flow<List<ConvertedFile>> {
        return convertedFileDao.getAllFilesInRoot()
    }


    fun getAllFavoriteFiles(): Flow<List<ConvertedFile>> {
        return convertedFileDao.getAllFavoriteFiles()
    }

    fun getAllFilesByName(): Flow<List<ConvertedFile>> {
        return  convertedFileDao.getAllFilesByName()
    }

    fun getAllFilesByType(): Flow<List<ConvertedFile>> {
        return convertedFileDao.getAllFilesByType()
    }

    fun getAllFilesByDateCreated(): Flow<List<ConvertedFile>> {
        return convertedFileDao.getAllFilesByDateCreated()
    }

    fun getAllFilesBySize(): Flow<List<ConvertedFile>> {
        return convertedFileDao.getAllFilesBySize()
    }

    fun filterAllFilesByType(types: List<String>): Flow<List<ConvertedFile>> {
        return convertedFileDao.filterAllFilesByType(types)
    }

    suspend fun insertFile(file: ConvertedFile) {
        Log.d("converter_files", "Inserting into database via DAO")
        return convertedFileDao.insertFile(file)
    }

    suspend fun deleteFile(file: ConvertedFile) {
        return convertedFileDao.deleteFile(file)
    }

    fun deleteAllFilesAndFoldersInPath(path: String) {
        convertedFileDao.deleteFilesAndFoldersInPath("$path%")
    }

    suspend fun deleteAll() {
        return convertedFileDao.deleteAll()
    }

    fun isFavorite(id: Long): Boolean {
        return convertedFileDao.isFavorite(id)
    }

    fun isDirectory(id: Long): Boolean {
        return convertedFileDao.isDirectory(id)
    }

    suspend fun updateFile(file: ConvertedFile) {
        return convertedFileDao.updateFile(file)
    }



}