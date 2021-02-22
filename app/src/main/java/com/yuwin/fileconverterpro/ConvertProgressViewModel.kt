package com.yuwin.fileconverterpro

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.util.*

class ConvertProgressViewModel(private val app: Application, val data: List<ConvertInfo>, private val quality: Int): AndroidViewModel(app) {

    private val _completePercentage = MutableLiveData<Int>()
    val completePercentage: LiveData<Int> get() = _completePercentage


    private val scope = CoroutineScope(SupervisorJob())
    private val storageDir = getExternalDir()
    private var itemPercentage: Int = 0


    init {
        itemPercentage = 100 / data.size
        _completePercentage.postValue(0)
    }

    fun convertImages() {
        val job = scope.launch {
            for(item in data) {
                launch(Dispatchers.Default) {
                    convertAndSaveImages(item, storageDir, quality)
                }.invokeOnCompletion {
                    increasePercentage()
                }
            }
        }.invokeOnCompletion {
            fillRemainingPercentage()
        }

    }




    private fun convertAndSaveImages(item: ConvertInfo, storageDir: String, quality: Int) {
        val inputStream = app.contentResolver.openInputStream(item.uri)
        try {
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val fileName = getFileName(item.fileName)
            val covertToExtension = getFileExtension(item.specificConvertFormat, item.defaultConvertFormat, item.convertAll)
            val fileSavePath = getFileSavePath(storageDir, fileName, covertToExtension)
            val fos = FileOutputStream(fileSavePath)
            performConvert(bitmap, covertToExtension, quality ,fos)

            inputStream?.close()
        }catch (e: Exception) {
            e.printStackTrace()
        }finally {
            inputStream?.close()
        }
    }

    private fun getFileExtension(specificFormat: Int?, defaultFormat: Int?, convertAll: Boolean?): String {
        return if(convertAll == true) {
            ".${FormatTypes.values()[defaultFormat!!].toString().toLowerCase(Locale.ROOT)}"
        }else {
            ".${FormatTypes.values()[specificFormat!!].toString().toLowerCase(Locale.ROOT)}"
        }

    }

    private fun getFileName(fileName: String): String {
        return File(fileName).nameWithoutExtension
    }

    private fun getFileSavePath(storageDir: String, fileName: String, fileExtension: String): String {
        return Util.getStoragePath(storageDir, fileName, fileExtension)
    }

    private fun getExternalDir(): String {
        return Util.getExternalDir(app.applicationContext)
    }

    fun progressToString(): String {
        return completePercentage.value.toString()
    }

    private fun performConvert(bitmap: Bitmap, to: String, quality: Int, filePath: FileOutputStream) {
        when(to) {
            ".jpg", ".jpeg" -> {
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, filePath)
            }
            ".png" -> {
                bitmap.compress(Bitmap.CompressFormat.PNG, quality, filePath)
            }

            ".webp" -> {
                bitmap.compress(Bitmap.CompressFormat.WEBP, quality, filePath)
            }
        }
    }

    private fun fillRemainingPercentage() {
        val currentPercentage = completePercentage.value
        if (currentPercentage != null) {
            if(currentPercentage < 100) {
                val remaining = 100 - currentPercentage
                _completePercentage.postValue(completePercentage.value?.plus(remaining))
            }
        }
    }

    private fun increasePercentage() {
        _completePercentage.postValue(completePercentage.value?.plus(itemPercentage))
    }

}