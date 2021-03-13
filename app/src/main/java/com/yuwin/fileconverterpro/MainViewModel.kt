package com.yuwin.fileconverterpro

import android.app.Application
import androidx.core.net.toUri
import androidx.lifecycle.*
import com.yuwin.fileconverterpro.db.AppDatabase
import com.yuwin.fileconverterpro.db.ConvertedFile
import com.yuwin.fileconverterpro.db.Repository
import com.yuwin.fileconverterpro.repository.DataStoreRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.*
import java.nio.channels.FileChannel
import java.util.*

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = DataStoreRepository(app)
    private val convertedFileDao = AppDatabase.getInstance(app).convertedFileDao()
    private val databaseRepository = Repository(convertedFileDao)

    private val externalDir = Util.getExternalDir(app.applicationContext)

    val readFiles = databaseRepository.getAllFiles().asLiveData()


    // Filter and Sort Queries

    val readFilesBySize = databaseRepository.getAllFilesBySize().asLiveData()
    val readFilesByName = databaseRepository.getAllFilesByName().asLiveData()
    val readFilesByType = databaseRepository.getAllFilesByType().asLiveData()
    val readFilesByDate = databaseRepository.getAllFilesByDateCreated().asLiveData()

    private val jpgJpeg = arrayListOf("jpg", "jpeg")
    val filterFilesByJpgJpeg = databaseRepository.filterAllFilesByType(jpgJpeg).asLiveData()

    private val pdf = arrayListOf("pdf")
    val filterFilesByPdf = databaseRepository.filterAllFilesByType(pdf).asLiveData()

    private val png = arrayListOf("png")
    val filterFilesByPng = databaseRepository.filterAllFilesByType(png).asLiveData()

    private val webp = arrayListOf("webp")
    val filterFilesByWebp = databaseRepository.filterAllFilesByType(webp).asLiveData()

//    DATABASE CRUD Operations

    suspend fun deleteSelectedFiles(file: ConvertedFile) {
        Util.deleteFileFromStorage(file)
        viewModelScope.launch {
            databaseRepository.deleteFile(file)
        }
    }


// Read/Write  Default  Quality Value

    val readQuality = repository.defaultQuality.asLiveData()

    fun setQuality(value: Int) {
        viewModelScope.launch {
            repository.setDefaultQuality(value)
        }
    }

    // Read/Write If review already prompted

    val readReviewPrompted = repository.reviewPrompted.asLiveData()

    fun setReviewPrompted(value: Boolean) {
        viewModelScope.launch {
            repository.setReviewPrompted(value)
        }
    }

    // Read/Write number of times app opened

    val readAppOpenedTimes = repository.openedTimes.asLiveData()

    fun incrementAppOpenedTimes() {
        viewModelScope.launch {
            repository.incrementOpenedTimes()
        }
    }

    // Read-Write default convert format

    val readDefaultFormat = repository.formatType.asLiveData()

    fun setFormatType(value: Int) {
        viewModelScope.launch {
            repository.setDefaultFormatType(value)
        }
    }


    // Read - Write Is Grid Enabled

    val readIfGridEnabled = repository.isGridEnabled.asLiveData()

    fun setIsGrid(value: Boolean) {
        viewModelScope.launch {
            repository.setIsGrid(value)
        }
    }

    // Read - Write Storage Directory

    val readCurrentStorage = repository.storageDirectory.asLiveData()

    fun setCurrentStorage(path: String) {
        viewModelScope.launch {
            repository.setStorageDirectory(path)
        }
    }

    // Read Write Premium Status

    val readIsPremium = repository.isPremiumMember.asLiveData()

    fun setPremiumStatus(value: Int) {
        viewModelScope.launch {
            repository.setPremium(value)
        }
    }


    // File Move and Copy Functionality

    fun moveFileOrDirectory(files: List<ConvertedFile>, to: String) {
        files.forEach { file ->
            if (file.isDirectory) {

            } else {
                moveFile(file, to)
            }
        }
    }

    private fun moveFile(
        file: ConvertedFile,
        to: String
    ) {
        var inDir = true
        val newFilePath = "${to}/${file.fileName}"
        File(file.filePath).renameTo(File(newFilePath))

        if (externalDir == to) {
            inDir = false
        }

        val newFile = file.apply {
            filePath = File(newFilePath).path
            uri = File(newFilePath).toUri()
            inDirectory = inDir
            isSelected = false
        }
        viewModelScope.launch {
            databaseRepository.updateFile(newFile)
        }

    }

    fun copyFileOrDirectory(files: List<ConvertedFile>, to: String) {

        files.forEach { file ->
            if (file.isDirectory) {

            } else {
                copyFile(file, to)
            }
        }
    }


    private fun copyFile(
        file: ConvertedFile,
        to: String
    ) {
        val dst = "${to}/${file.fileName}"
        copyFileOrDirectory(file.filePath, dst)

        val millis = Util.getCurrentTimeMillis()
        val fileName = file.fileName
        val newFileUri = File(dst).toUri()
        val date = Date(millis.toLong())
        val thumbNailUri = file.thumbnailUri
        var inDir = true

        if (externalDir == to) {
            inDir = false
        }

        val newConvertedFile = ConvertedFile(
            0,
            fileName,
            file.fileSize,
            file.fileSizeValue,
            dst,
            file.fileType,
            newFileUri,
            thumbNailUri,
            isFavorite = false,
            isSelected = false,
            isDirectory = false,
            inDirectory = inDir,
            null,
            date
        )

        viewModelScope.launch {
            databaseRepository.insertFile(newConvertedFile)
        }
    }

    private fun copyFileOrDirectory(src: String, dst: String) {

        var source: FileChannel? = null
        var destination: FileChannel? = null

        try {
            source = FileInputStream(File(src)).channel
            destination = FileOutputStream(File(dst)).channel
            destination.transferFrom(source, 0, source.size())

        } finally {
            source?.close()
            destination?.close()
        }

    }


}