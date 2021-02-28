package com.yuwin.fileconverterpro

import android.app.Application
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.*
import com.yuwin.fileconverterpro.db.AppDatabase
import com.yuwin.fileconverterpro.db.ConvertedFile
import com.yuwin.fileconverterpro.db.Repository
import kotlinx.coroutines.*
import java.io.*
import java.util.*


class ConvertProgressViewModel(private val app: Application, val data: List<ConvertInfo>, private val quality: Int): AndroidViewModel(app) {

    private val _completePercentage = MutableLiveData<Int>()
    val completePercentage: LiveData<Int> get() = _completePercentage

    private val _conversionFinished = MutableLiveData<Boolean>()
    val conversionFinished: LiveData<Boolean> get() = _conversionFinished

    private val _convertedFileName = MutableLiveData<String>()
    val convertedFileName: LiveData<String> get() = _convertedFileName

    private val _convertedProgressMessage = MutableLiveData<String>()
    val convertedProgressMessage: LiveData<String> get() =  _convertedProgressMessage

    private val dao = AppDatabase.getInstance(app).convertedFileDao()
    private val repository = Repository(dao)


    private val scope = CoroutineScope(SupervisorJob())
    private val storageDir = getExternalDir()
    private var itemPercentage: Int = 0


    init {
        _convertedProgressMessage.postValue("Your files are being converted")
        _completePercentage.postValue(0)
        _conversionFinished.postValue(false)

    }

    fun convertFiles() {
        _completePercentage.postValue(0)
        itemPercentage = 80 / data.size
        val job = scope.launch {
            for(item in data) {
                launch(Dispatchers.Default) {
                    convertAndSaveFiles(item, storageDir, quality)
                }.invokeOnCompletion {
                    increasePercentage(itemPercentage)
                    _convertedFileName.postValue("${item.fileName}:Converted")
                }
            }
        }.invokeOnCompletion {
            fillRemainingPercentage()
            _convertedProgressMessage.postValue("File Conversion Complete")
            _conversionFinished.postValue(true)
        }

    }

    fun createMultiPagePdf() {
        _completePercentage.postValue(0)
        val job = scope.launch {
                launch(Dispatchers.Default) {
                    createAndSaveMultiPagePdf(data, storageDir)
                }

        }.invokeOnCompletion {
            fillRemainingPercentage()
            _conversionFinished.postValue(true)
            _convertedProgressMessage.postValue("File Conversion Complete")
        }
    }


    private fun convertAndSaveFiles(item: ConvertInfo, storageDir: String, quality: Int) {
        val inputStream = app.contentResolver.openInputStream(item.uri)
        try {
            val options = BitmapFactory.Options()
            options.inScaled = false
            val bitmap = BitmapFactory.decodeStream(inputStream,null, options)
            val convertToExtension = Util.getFileExtension(item.specificConvertFormat, item.defaultConvertFormat, item.convertAll)
            val fileName = getFileName(item.fileName)
            val fileSavePath = getFileSavePath(storageDir, fileName, convertToExtension)
            if (bitmap != null) {
                performConvert(bitmap, convertToExtension, quality, fileSavePath)
            }

            if(convertToExtension != ".pdf") {
                val file = createConvertedImageFile(item, fileSavePath, convertToExtension)
                viewModelScope.launch(Dispatchers.IO) {
                    repository.insertFile(file)
                }
            }

            inputStream?.close()
        }catch (e: Exception) {
            e.printStackTrace()
        }finally {
            inputStream?.close()
        }
    }

    private fun performConvert(bitmap: Bitmap, to: String, quality: Int, filePath: String) {
        when(to) {
            ".jpg", ".jpeg" -> {
                Log.d("convertDebug", "In Jpg/Jpeg $filePath")

                val fos = FileOutputStream(filePath)
                try {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, fos)
                } catch (e: java.lang.Exception) {

                } finally {
                    fos.close()
                }
            }
            ".png" -> {
                Log.d("convertDebug", "In Png $filePath")

                val fos = FileOutputStream(filePath)
                try {
                    bitmap.compress(Bitmap.CompressFormat.PNG, quality, fos)
                } catch (e: java.lang.Exception) {

                } finally {
                    fos.close()
                }

            }
            ".webp" -> {
                Log.d("convertDebug", "In Webp $filePath")
                val fos = FileOutputStream(filePath)
                try {
                    bitmap.compress(Bitmap.CompressFormat.WEBP, quality, fos)
                } catch (e: java.lang.Exception) {
                } finally {
                    fos.close()
                }
            }
            ".pdf" -> {
                Log.d("convertDebug", "In Pdf $filePath")
                createAndSaveSinglePagePdf(bitmap, this.storageDir)
            }


        }
    }


    private fun createAndSaveSinglePagePdf(bitmap: Bitmap, storageDir: String) {
        val fileName = Util.getCurrentTimeMillis()
        val filePath = "${storageDir}${fileName}.pdf"
        val document = PdfDocument()
        val pageInfo: PdfDocument.PageInfo  = PdfDocument.PageInfo.Builder(bitmap.width + 50, bitmap.height + 50, 1).setContentRect(Rect(0, 0, bitmap.width + 50, bitmap.height + 50)).create()
        val page: PdfDocument.Page  = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        canvas.drawBitmap(bitmap, 25f, 25f, null)
        document.finishPage(page)
        var thumbNail: Bitmap? = scaleBitmapToAspectRatio(bitmap, 132,132)


        val fos =  getOutputStream(filePath)
        try {
            writeDocument(document, fos)
            var thumbNailUri: Uri? = null
            if(thumbNail != null) {
                thumbNailUri = savePdfThumbNail(thumbNail, fileName)
            }

            val file = createConvertedPdfFile(fileName, filePath, thumbNailUri)



            viewModelScope.launch(Dispatchers.IO) {
                repository.insertFile(file)
            }
        }
        catch (e: FileNotFoundException){
            e.printStackTrace()
        }
        catch (e: IOException) {
            e.printStackTrace()
        }
        finally {
            fos.close()
            document.close()
        }
    }

    private suspend fun createAndSaveMultiPagePdf(items: List<ConvertInfo>, storageDir: String) {
        itemPercentage = (80 / items.size)
        val fileName = Util.getCurrentTimeMillis()
        val filePath = "${storageDir}${fileName}.pdf"
        val document = PdfDocument()
        val (width, height) = withContext(Dispatchers.Default) { getMaxWidthAndHeight(items) }
        var thumbNail: Bitmap? = null

        items.forEachIndexed { index, item ->
            val inputStream = app.contentResolver.openInputStream(item.uri)
            try {
                var bitmap = withContext(Dispatchers.IO) { BitmapFactory.decodeStream(inputStream) }
                if (index == 0) {
                    thumbNail = withContext(Dispatchers.Default) {scaleBitmapToAspectRatio(bitmap, 132,132)}
                }

                bitmap = withContext(Dispatchers.Default) { scaleBitmapToAspectRatio(bitmap, height, width) }


                val pageInfo: PdfDocument.PageInfo  = PdfDocument.PageInfo.Builder(width + 50, height + 50, index + 1).setContentRect(Rect(0, 0, width + 50, height + 50)).create()
                val page: PdfDocument.Page  = document.startPage(pageInfo)
                val canvas: Canvas = page.canvas
                val xOffset = ((canvas.width - bitmap.width)/ 2).toFloat()
                val yOffset = ((canvas.height - bitmap.height)/ 2).toFloat()
                val canvasColor = Paint(ContextCompat.getColor(app.applicationContext, R.color.pdfBackground))
                canvas.drawBitmap(bitmap, xOffset, yOffset, canvasColor)
                document.finishPage(page)
                _convertedFileName.postValue("${item.fileName}:Converted")
                increasePercentage(itemPercentage)
            }catch (e: Exception) {
                e.printStackTrace()
            }finally {
                inputStream?.close()
            }

        }


        try {

            var thumbNailUri: Uri? = null
            if(thumbNail != null) {
                withContext(Dispatchers.IO) {
                    thumbNailUri =  savePdfThumbNail(thumbNail!!, fileName)
                }
            }

            val fos = withContext(Dispatchers.IO) {
                getOutputStream(filePath)
            }

            withContext(Dispatchers.IO) { writeDocument(document, fos) }
            val file = createConvertedPdfFile(fileName, filePath, thumbNailUri)

            viewModelScope.launch(Dispatchers.IO) {
                repository.insertFile(file)
            }
        }
        catch (e: FileNotFoundException){
            e.printStackTrace()
        }
        catch (e: IOException) {
            e.printStackTrace()
        }
        finally {
            document.close()
        }
    }

    private fun savePdfThumbNail(bitmap: Bitmap, fileName: String): Uri? {
        val folder = File(storageDir, "PDF")
        if(!folder.exists()) {
            folder.mkdir()
        }

        val file = File(folder, "$fileName.png")
        var fos: FileOutputStream? = null

        try {
            fos = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.close()
            return file.toUri()
        }catch (e: Exception) {
            e.printStackTrace()
        }finally {
            fos?.close()
        }
        return null
    }

    private fun getOutputStream(filePath: String): FileOutputStream {
        _convertedFileName.postValue("Creating file...")
        val fos: FileOutputStream?
        fos = FileOutputStream(filePath)
        increasePercentage()
        return fos
    }

    private fun writeDocument(doc: PdfDocument, fos: FileOutputStream) {
        _convertedFileName.postValue("Writing data into file...")
        doc.writeTo(fos)
        increasePercentage()
    }

    private fun getMaxWidthAndHeight(items: List<ConvertInfo>): Pair<Int, Int> {
        var width  = 0
        var height = 0
        for(item in items) {
            val inputStream = app.contentResolver.openInputStream(item.uri)

            try {
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if(width < bitmap.width)
                    width = bitmap.width

                if (height < bitmap.height)
                    height = bitmap.height

            }catch (e: Exception){
                e.printStackTrace()
            }finally {
                inputStream?.close()
            }
        }

        return Pair(width, height)
    }

    private fun scaleBitmapToAspectRatio(targetBmp: Bitmap, reqHeightInPixels: Int, reqWidthInPixels: Int): Bitmap? {
        val matrix = Matrix()
        matrix.setRectToRect(RectF(0F, 0F, targetBmp.width.toFloat(), targetBmp.height.toFloat()), RectF(0F, 0F, reqWidthInPixels.toFloat(), reqHeightInPixels.toFloat()), Matrix.ScaleToFit.CENTER)
        return Bitmap.createBitmap(targetBmp, 0, 0, targetBmp.width, targetBmp.height, matrix, true)
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

    private fun fillRemainingPercentage() {
        val currentPercentage = completePercentage.value
        if (currentPercentage != null) {
            if(currentPercentage < 100) {
                val remaining = 100 - currentPercentage
                _completePercentage.postValue(completePercentage.value?.plus(remaining))
            }
        }
    }

    private fun createConvertedImageFile(item: ConvertInfo, filePath: String, extension: String): ConvertedFile {
        val millis = Util.getCurrentTimeMillis()
        val fileName = File(filePath).name
        val uri = File(filePath).toUri()
        val date = Date(millis.toLong())
        val fileSize = Util.convertBytes(File(filePath).length())
        return ConvertedFile(0, fileName, fileSize, filePath, extension, uri, null, isFavorite = false, isSelected = false, date)
    }

    private fun createConvertedPdfFile(fileName: String, filePath: String, thumbNailUri: Uri?): ConvertedFile {
        val millis = Util.getCurrentTimeMillis()
        val uri = File(filePath).toUri()
        val date = Date(millis.toLong())
        val fileSize = Util.convertBytes(File(filePath).length())
        return ConvertedFile(0, fileName, fileSize , filePath, "pdf", uri, thumbNailUri, isFavorite = false, isSelected = false, date)
    }

    private fun increasePercentage(value: Int = 9) {
        _completePercentage.postValue(completePercentage.value?.plus(value))
    }

    override fun onCleared() {
        super.onCleared()
        _completePercentage.postValue(0)
        scope.cancel()
    }

}