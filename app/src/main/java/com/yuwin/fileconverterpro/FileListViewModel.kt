package com.yuwin.fileconverterpro

import android.app.Application
import android.view.View
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.material.snackbar.Snackbar
import com.yuwin.fileconverterpro.db.AppDatabase
import com.yuwin.fileconverterpro.db.ConvertedFile
import com.yuwin.fileconverterpro.db.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

class FileListViewModel(private val app: Application): AndroidViewModel(app) {

    private val database: AppDatabase = AppDatabase.getInstance(app.applicationContext)
    private val repository: Repository = Repository(database.convertedFileDao())
    private val scope = CoroutineScope(SupervisorJob())

    val readFiles = repository.getAllFiles().asLiveData()


    fun clearDatabase() {
        viewModelScope.launch {
            repository.deleteAll()
            val dir = File(Util.getExternalDir(app))
            val dirPdf = File(Util.getExternalDir(app), "PDF")
            if (dir.isDirectory && dir.exists()) {
                val children: Array<String> = dir.list()!!
                for (i in children.indices) {
                    File(dir, children[i]).delete()
                }
            }
            if(dirPdf.exists() && dirPdf.isDirectory) {
                val children: Array<String> = dirPdf.list()!!
                for (i in children.indices) {
                    File(dirPdf, children[i]).delete()
                }
            }
        }

    }

    fun deleteSelectedFiles(file: ConvertedFile) {
        viewModelScope.launch {
                Util.deleteFileFromStorage(file)
                repository.deleteFile(file)

        }
    }




}