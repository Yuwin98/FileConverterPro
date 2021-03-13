package com.yuwin.fileconverterpro

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.yuwin.fileconverterpro.db.AppDatabase
import com.yuwin.fileconverterpro.db.ConvertedFile
import com.yuwin.fileconverterpro.db.Repository
import kotlinx.coroutines.launch
import java.io.File

class FileListViewModel(private val app: Application) : AndroidViewModel(app) {

    private val database: AppDatabase = AppDatabase.getInstance(app.applicationContext)
    private val repository: Repository = Repository(database.convertedFileDao())

    val readFiles = repository.getAllFiles().asLiveData()




    fun clearDatabase() {
        viewModelScope.launch {
            repository.deleteAll()
            val dir = File(Util.getExternalDir(app))
            val dirPdf = File(Util.getExternalDir(app), "PDF")
            if (dir.isDirectory && dir.exists()) {
                val children: Array<String> = dir.list()!!
                for (i in children.indices) {
                    if (File(dir, children[i]).isDirectory) {
                        clearDir(File(dir, children[i]))
                    } else {
                        File(dir, children[i]).delete()
                    }
                }
            }
            if (dirPdf.exists() && dirPdf.isDirectory) {
                val children: Array<String> = dirPdf.list()!!
                for (i in children.indices) {
                    File(dirPdf, children[i]).delete()
                }

            }
        }

    }

    private fun clearDir(directory: File) {
        val files = directory.listFiles()
        viewModelScope.launch {
            files?.forEach {
                File(it.path).delete()
            }
        }
        directory.delete()
    }




    fun updateNewFile(file: ConvertedFile) {
        viewModelScope.launch {
            repository.updateFile(file)
        }
    }

    fun insertFolder(folder: ConvertedFile) {
        viewModelScope.launch {
            repository.insertFile(folder)
        }
    }


}