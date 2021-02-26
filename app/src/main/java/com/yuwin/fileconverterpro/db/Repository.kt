package com.yuwin.fileconverterpro.db

import android.util.Log
import kotlinx.coroutines.flow.Flow

class Repository(private val convertedFileDao: ConvertedFileDao) {

    fun getAllFiles(): Flow<List<ConvertedFile>> {
        return convertedFileDao.getAllFiles()
    }

    suspend fun insertFile(file: ConvertedFile) {
        Log.d("converter_files", "Inserting into database via DAO")
        return convertedFileDao.insertFile(file)
    }

    suspend fun deleteFile(file: ConvertedFile) {
        return convertedFileDao.deleteFile(file)
    }

    suspend fun deleteAll() {
        return convertedFileDao.deleteAll()
    }

    fun isFavorite(id: Long): Boolean {
        return convertedFileDao.isFavorite(id)
    }

    fun getCount(): Int {
        return convertedFileDao.count()
    }

    suspend fun updateFile(file: ConvertedFile) {
        return convertedFileDao.updateFile(file)
    }



}