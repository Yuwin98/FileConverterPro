package com.yuwin.fileconverterpro

import android.app.Application
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.yuwin.fileconverterpro.db.AppDatabase
import com.yuwin.fileconverterpro.db.Repository
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.File


class FileListViewModel(private val app: Application) : AndroidViewModel(app) {

    private val database: AppDatabase = AppDatabase.getInstance(app.applicationContext)
    private val repository: Repository = Repository(database.convertedFileDao())

    val readFiles = repository.getAllFiles().asLiveData()
    val readFilesInRoot = repository.getAllFilesInRoot().asLiveData()


    fun clearDatabase() {
        viewModelScope.launch {
            val dir = File(Util.getExternalDir(app))
            repository.getAllFiles().collect {
                it.forEach { file ->
                    if (!file.isDirectory && file.publicUri != null || file.publicUri != Uri.EMPTY) {
                        file.publicUri?.let { it1 ->
                            Util.deleteFileFromPublicStorage(
                                app.applicationContext,
                                it1
                            )
                        }
                    }
                }
                repository.deleteAll()
                dir.deleteRecursively()
            }


        }

    }


}