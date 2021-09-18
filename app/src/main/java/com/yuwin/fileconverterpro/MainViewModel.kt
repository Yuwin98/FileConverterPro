package com.yuwin.fileconverterpro

import android.app.Application
import android.content.Context
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.net.toUri
import androidx.lifecycle.*
import com.yuwin.fileconverterpro.db.AppDatabase
import com.yuwin.fileconverterpro.db.ConvertedFile
import com.yuwin.fileconverterpro.db.Repository
import com.yuwin.fileconverterpro.repository.DataStoreRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.*
import java.nio.channels.FileChannel
import java.util.*

class MainViewModel(private val app: Application) : AndroidViewModel(app) {

    private val repository = DataStoreRepository(app)
    private val convertedFileDao = AppDatabase.getInstance(app).convertedFileDao()
    private val databaseRepository = Repository(convertedFileDao)

    private val _scrollPosition = MutableLiveData<Int>()

    private var externalDir = Util.getExternalDir(app.applicationContext).substringBeforeLast('/')


    val readAllDirectoryFiles = databaseRepository.getAllFilesInDirectory().asLiveData()




//    DATABASE CRUD Operations

    fun searchDatabaseInRoot(query: String): LiveData<List<ConvertedFile>> {
        return databaseRepository.searchDatabaseInRoot(query).asLiveData()
    }

    fun searchDatabaseInDirectory(query: String, filePath: String): LiveData<List<ConvertedFile>> {
        return databaseRepository.searchDatabaseInDirectory(query,filePath).asLiveData()
    }

    suspend fun deleteSelectedFiles(file: ConvertedFile, context: Context?) {
        Util.deleteFileFromStorage(file, context)
        viewModelScope.launch(Dispatchers.IO) {
            databaseRepository.deleteFile(file)
        }
    }

     fun getAllFilesInStorageFolder(filePath: String): Flow<List<ConvertedFile>> {
        return databaseRepository.getAllDirectoryFilesWithFilePath(filePath)
    }

    fun deleteFilesAndFoldersFromPath(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            databaseRepository.deleteAllFilesAndFoldersInPath(path)
        }
    }

    fun getAllDirectoryFilesWithPath(path: String): LiveData<List<ConvertedFile>> {
        return databaseRepository.getAllDirectoryFilesWithFilePath(path).asLiveData()
    }

    // Rename a file
    fun renameFile(convertedFile: ConvertedFile, newName: String) {
        val newFileNameWithExtension = "$newName.${convertedFile.fileType}"
        val fileParent = File(convertedFile.filePath).parent + "/"

        val newPath = Util.getStoragePathWithExtension(
            fileParent,
            newName,
            ".${convertedFile.fileType}"
        )
        val newUri = File(newPath).toUri()

        File(convertedFile.filePath).renameTo(File(newPath))

        val file = convertedFile.apply {
            fileName = newFileNameWithExtension
            filePath = newPath
            uri = newUri
        }
        viewModelScope.launch {
            databaseRepository.updateFile(file)
        }


    }




    // Update a converted file
    fun updateFile(file: ConvertedFile) {
        viewModelScope.launch(Dispatchers.IO) {
            databaseRepository.updateFile(file)
        }
    }


    // Create Empty File
    fun createFileDirectory(
        name: String,
        selectedFileSize: Int,
        currentDir: String,
        inDir: Boolean
    ): ConvertedFile? {
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

    // File name validation checks

    fun checkIfFileNameUnique(fileToRename: ConvertedFile, name: String): Boolean {
        val fileParent = File(fileToRename.filePath).parent
        val files = File(fileParent).listFiles().map { it.name }
        val checkAgainst = name + "." + fileToRename.fileType
        return files.contains(checkAgainst)
    }

    fun checkNewFolderNameUnique(file: File, name: String): Boolean {
        if(!file.isDirectory) {
            return false
        }
        val files = file.listFiles().map { it.name }
        return files.contains(name)
    }

    fun checkIfFileNameValid(name: String): Boolean {
        val fileName = "[a-zA-Z0-9][a-zA-Z0-9_ -]*[a-zA-Z0-9_-]"
        val regex = Regex(fileName)
        return regex.matches(name)
    }

    fun checkFileNameTooLong(newName: String): Boolean {
        return newName.length > 30
    }

    fun checkFileNameTooShort(newName: String): Boolean {
        return newName.length < 2
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
    // Deep Filter function
   fun deepFilter(type: FILTER, items: List<ConvertedFile>): List<ConvertedFile> {
        when (type) {
            FILTER.JPG -> {
                return items.filter { it.fileType == "jpg" }
            }
            FILTER.PNG -> {
                return items.filter { it.fileType == "png" }
            }
            FILTER.PDF_FILE -> {
                return items.filter { it.fileType == "pdf" }
            }
            FILTER.WEBP -> {
                return items.filter { it.fileType == "webp" }
            }
            FILTER.DATE -> {
                return items.sortedWith(compareBy { it.date }).reversed()
            }
            FILTER.TYPE -> {
                return items.sortedWith(compareBy<ConvertedFile> { it.fileType }.thenByDescending { it.date })
            }
            FILTER.SIZE -> {
                return items.sortedByDescending { Util.retrieveFileSize(File(it.filePath)) }
            }
            FILTER.NAME -> {
                return items.sortedBy { it.fileName }
            }
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

    val readCurrentStorage = Environment.DIRECTORY_PICTURES + File.separator + "Image Converter"

    // Read Write Premium Status

    val readIsPremium = repository.isPremiumMember.asLiveData()

    fun setPremiumStatus(value: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setPremium(value)
        }
    }

    private val _isLoading = MutableLiveData(true)
    val isLoading: LiveData<Boolean> = _isLoading

    fun setIsLoading(loading: Boolean) {
        _isLoading.value = loading
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
        val dst = "${to}/${file.fileName}"

        if(File(dst).exists()) {
           viewModelScope.launch {
                Toast.makeText(app, "${file.fileName} already exist on destination. Rename before moving", Toast.LENGTH_SHORT).show()
            }
            return
        }

        File(file.filePath).renameTo(File(dst))

        if (externalDir == File(dst).parent) {
            inDir = false
        }

        val newFile = file.apply {
            filePath = File(dst).path
            uri = File(dst).toUri()
            inDirectory = inDir
            isSelected = false
        }
        viewModelScope.launch(Dispatchers.IO) {
            databaseRepository.updateFile(newFile)
        }

    }

    private fun moveDirectory(file: ConvertedFile, to: String): String {
        var inDir = true
        val dst = "${to}/${file.fileName}"
        if (!File(dst).exists()) {
            File(dst).mkdir()
        } else {
            return dst
        }

        if (externalDir == File(dst).parent) {
            inDir = false
        }
        Log.d("MoveFiles", "Directory: $inDir")

        val newFile = file.apply {
            filePath = File(dst).path
            uri = File(dst).toUri()
            inDirectory = inDir
            isSelected = false
        }
        viewModelScope.launch(Dispatchers.IO) {
            databaseRepository.updateFile(newFile)
        }
        return dst
    }

    fun copyFileOrDirectory(
        files: List<ConvertedFile>,
        dirFileList: List<ConvertedFile>,
        to: String

    ) {
        val millis = Util.getCurrentTimeMillis()
        files.forEach { file ->
            if (file.isDirectory) {
                val filterFilesInDirectory =
                    dirFileList.let { Util.filterItemsInDirectory(File(file.filePath), it) }
                val directoryPath = copyDirectory(file, to, millis)
                copyFileOrDirectory(filterFilesInDirectory, dirFileList, directoryPath)
            } else {
                copyConvertedFile(file, to, millis)
            }
        }
    }


    private fun copyDirectory(file: ConvertedFile, to: String, millis: String): String {

        val dst = "${to}/${file.fileName}"
        if (!File(dst).exists()) {
            File(dst).mkdir()
        } else {
            return dst
        }

        val fileName = file.fileName
        val newFileUri = File(dst).toUri()
        val date = Date(millis.toLong())
        val thumbNailUri = file.thumbnailUri
        var inDir = true

        if (externalDir == File(dst).parent) {
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
            null,
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


    private fun copyConvertedFile(
        file: ConvertedFile,
        to: String,
        millis: String
    ) {
        val dst = "${to}/${file.fileName}"

        if(File(dst).exists()) {
            viewModelScope.launch {
                Toast.makeText(app, "${file.fileName} already exist on destination. Rename before coping", Toast.LENGTH_SHORT).show()
            }
            return
        }

        copyFiles(file.filePath, dst)

        val fileName = file.fileName
        val newFileUri = File(dst).toUri()
        val date = Date(millis.toLong())
        val thumbNailUri = file.thumbnailUri
        var inDir = true

        if (externalDir == File(dst).parent) {
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
            null,
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