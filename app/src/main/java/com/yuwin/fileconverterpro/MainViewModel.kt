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
    val readAllDirectoryFiles = databaseRepository.getAllFilesInDirectory().asLiveData()


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
        viewModelScope.launch(Dispatchers.IO) {
            databaseRepository.deleteFile(file)
        }
    }

    // Update a converted file
    fun updateNewFile(file: ConvertedFile) {
        viewModelScope.launch(Dispatchers.IO) {
            databaseRepository.updateFile(file)
        }
    }


    // Create Empty File
    fun createFileDirectory(name: String, selectedFileSize: Int, currentDir:String, inDir: Boolean): ConvertedFile? {
        try {
            val dir = "$currentDir/"
            val folderPath = Util.getStorageFolder(dir, name)
            val contentSize = Util.getContentSize(selectedFileSize)
            val directoryColor = (0..24).random()
            val date = Date(Util.getCurrentTimeMillis().toLong())
            val file = File(folderPath)
            if (!file.exists()) {
                file.mkdir()
            }

            val folder = ConvertedFile(
                0,
                name,
                contentSize,
                0,
                file.path,
                "Directory",
                file.toUri(),
                null,
                isFavorite = false,
                isSelected = false,
                isDirectory = true,
                inDir,
                directoryColor,
                date
            )
            viewModelScope.launch(Dispatchers.IO) {
                databaseRepository.insertFile(folder)
            }

            return if (file.exists()) folder else null
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }


// Read/Write  Default  Quality Value

    val readQuality = repository.defaultQuality.asLiveData()

    fun setQuality(value: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setDefaultQuality(value)
        }
    }

    // Read/Write If review already prompted

    val readReviewPrompted = repository.reviewPrompted.asLiveData()

    fun setReviewPrompted(value: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setReviewPrompted(value)
        }
    }

    // Read/Write number of times app opened

    val readAppOpenedTimes = repository.openedTimes.asLiveData()

    fun incrementAppOpenedTimes() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.incrementOpenedTimes()
        }
    }

    // Read-Write default convert format

    val readDefaultFormat = repository.formatType.asLiveData()

    fun setFormatType(value: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setDefaultFormatType(value)
        }
    }


    // Read - Write Is Grid Enabled

    val readIfGridEnabled = repository.isGridEnabled.asLiveData()

    fun setIsGrid(value: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setIsGrid(value)
        }
    }

    // Read - Write Storage Directory

    val readCurrentStorage = repository.storageDirectory.asLiveData()

    fun setCurrentStorage(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setStorageDirectory(path)
        }
    }

    // Read Write Premium Status

    val readIsPremium = repository.isPremiumMember.asLiveData()

    fun setPremiumStatus(value: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setPremium(value)
        }
    }


    // File Move and Copy Functionality

    fun moveFileOrDirectory(
        files: List<ConvertedFile>,
        dirFileList: List<ConvertedFile>,
        to: String
    ) {
        files.forEach { file ->
            if (file.isDirectory) {
                val filePath = file.filePath
                val filterFilesInDirectory =
                    dirFileList.let { Util.filterItemsInDirectory(File(file.filePath), it) }
                val directoryPath = moveDirectory(file, to)
                moveFileOrDirectory(filterFilesInDirectory, dirFileList, directoryPath)
                File(filePath).delete()

            } else {
                moveConvertedFile(file, to)
            }
        }

    }

    private fun moveConvertedFile(
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
        viewModelScope.launch(Dispatchers.IO) {
            databaseRepository.updateFile(newFile)
        }

    }

    private fun moveDirectory(file: ConvertedFile, to: String): String {
        var inDir = true
        val newFilePath = "${to}/${file.fileName}"
        if (!File(newFilePath).exists()) {
            File(newFilePath).mkdir()
        }

        if (externalDir == to) {
            inDir = false
        }

        val newFile = file.apply {
            filePath = File(newFilePath).path
            uri = File(newFilePath).toUri()
            inDirectory = inDir
            isSelected = false
        }
        viewModelScope.launch(Dispatchers.IO) {
            databaseRepository.updateFile(newFile)
        }
        return newFilePath
    }

    fun copyFileOrDirectory(
        files: List<ConvertedFile>,
        dirFileList: List<ConvertedFile>,
        to: String
    ) {
        files.forEach { file ->
            if (file.isDirectory) {
                val filterFilesInDirectory =
                    dirFileList.let { Util.filterItemsInDirectory(File(file.filePath), it) }
                val directoryPath = copyDirectory(file, to)
                copyFileOrDirectory(filterFilesInDirectory, dirFileList, directoryPath)
            } else {
                createNewConvertedFile(file, to)
            }
        }
    }


    private fun copyDirectory(file: ConvertedFile, to: String): String {

        val dst = "${to}/${file.fileName}"
        if (!File(dst).exists()) {
            File(dst).mkdir()
        }

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
            isDirectory = true,
            inDirectory = inDir,
            file.directoryColor,
            date
        )

        viewModelScope.launch(Dispatchers.IO) {
            databaseRepository.insertFile(newConvertedFile)
        }

        return dst
    }


    private fun createNewConvertedFile(
        file: ConvertedFile,
        to: String
    ) {
        val dst = "${to}/${file.fileName}"
        copyFiles(file.filePath, dst)

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

        viewModelScope.launch(Dispatchers.IO) {
            databaseRepository.insertFile(newConvertedFile)
        }
    }

    private fun copyFiles(src: String, dst: String) {

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