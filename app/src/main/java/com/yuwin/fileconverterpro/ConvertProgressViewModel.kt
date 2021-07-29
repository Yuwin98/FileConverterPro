package com.yuwin.fileconverterpro

import android.app.Application
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.yuwin.fileconverterpro.db.AppDatabase
import com.yuwin.fileconverterpro.db.ConvertedFile
import com.yuwin.fileconverterpro.db.Repository
import kotlinx.coroutines.*
import java.io.*
import java.util.*


class ConvertProgressViewModel(
    private val app: Application,
    val data: List<ConvertInfo>,
    private val quality: Int,
    private val padding: Int,
    private val fileQuality: Int,
    private val pdfPageSize: Int,
    private val convertInto: String,
    private val pageInfoList: SelectedPageInfoList?
) : AndroidViewModel(app) {

    private val _completePercentage = MutableLiveData<Double>()
    val completePercentage: LiveData<Double> get() = _completePercentage

    private val _conversionFinished = MutableLiveData<Boolean>()
    val conversionFinished: LiveData<Boolean> get() = _conversionFinished

    private val _convertedFileName = MutableLiveData<String>()
    val convertedFileName: LiveData<String> get() = _convertedFileName

    private val _convertedProgressMessage = MutableLiveData<String>()
    val convertedProgressMessage: LiveData<String> get() = _convertedProgressMessage

    private val _conversionPaused = MutableLiveData<Boolean>()
    private val conversionPaused: LiveData<Boolean> get() = _conversionPaused

    private val dao = AppDatabase.getInstance(app).convertedFileDao()
    private val repository = Repository(dao)

    private val convertedFileList = mutableListOf<ConvertedFile>()

    private val currentConvertedFiles = mutableListOf<ConvertedFile>()
    private var imageFolderPath = ""


    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var job: DisposableHandle

    private var pausedAt: Int = 0
    private var currentIndex: Int = 0
    private var originalSize: Int = data.size
    private val maxPercentage = 95.0

    private val storageDir = getExternalDir()
    private var itemPercentage: Double = 0.0


    init {
        _completePercentage.postValue(2.0)
        _conversionFinished.postValue(false)
    }

    fun convertFiles() {
        var data = this.data
        itemPercentage = (maxPercentage / originalSize)
        if (conversionPaused.value == true) {
            data = data.subList(pausedAt, originalSize)
            this.scope = CoroutineScope(SupervisorJob())
        }
        scope.launch {

            data.forEachIndexed { index, convertInfo ->
                val job = async {
                    convertAndSaveFiles(convertInfo, storageDir, quality)
                }
                job.join()
                if (job.isCompleted) {
                    if (!job.isCancelled) {
                        currentIndex = index + pausedAt + 1
                        _convertedFileName.postValue("${convertInfo.fileName} - Converted ")
                        _convertedProgressMessage.postValue("$currentIndex out of $originalSize  Converted")
                        increasePercentage(itemPercentage)
                    }
                }
            }

        }.invokeOnCompletion { throwable ->
            if (throwable is CancellationException) {
                pausedAt = currentIndex
                _conversionPaused.postValue(true)
                _convertedProgressMessage.postValue("$currentIndex out of $originalSize  Converted")
                _convertedFileName.postValue("Conversion Paused")

            } else {
                fillRemainingPercentage()
                _conversionFinished.postValue(true)
                _convertedProgressMessage.postValue("File Conversion Completed: $currentIndex out of $originalSize  Converted")
            }
        }

    }

    fun createMultiPagePdf() {

        val data = data
        _completePercentage.postValue(0.0)

        if (conversionPaused.value == true) {
            this.scope = CoroutineScope(Job())
            _conversionPaused.postValue(false)
        }

        _convertedProgressMessage.postValue("Conversion starting...")

        job = scope.launch {
            _convertedProgressMessage.postValue("Your files are being converted")
            withContext(Dispatchers.Default) {
                createAndSaveMultiPagePdf(data, storageDir)
            }


        }.invokeOnCompletion { throwable ->
            if (throwable is CancellationException) {
                _conversionPaused.postValue(true)
                _convertedFileName.postValue("Conversion Stopped")
            } else {
                fillRemainingPercentage()
                _conversionFinished.postValue(true)
                _convertedProgressMessage.postValue("File Conversion Complete")
            }
        }
    }

    fun mergePDF() {
        val data = this.data

        if (conversionPaused.value == true) {
            this.scope = CoroutineScope(Job())
            _conversionPaused.postValue(false)
        }

        _convertedProgressMessage.postValue("Merging starting...")

        job = scope.launch {
            _convertedProgressMessage.postValue("Your files are being merged")
            withContext(Dispatchers.Default) {
                mergeAndSavePdfFile(data, storageDir)
            }


        }.invokeOnCompletion { throwable ->
            if (throwable is CancellationException) {
                _conversionPaused.postValue(true)
                _convertedFileName.postValue("Merging Stopped")
            } else {
                fillRemainingPercentage()
                _conversionFinished.postValue(true)
                _convertedProgressMessage.postValue("File Merging Complete")
            }
        }
    }


    private suspend fun mergeAndSavePdfFile(data: List<ConvertInfo>, storageDir: String) {
        val getPageSize = getPdfQuality(0)
        val pageWidth = getPageSize.first
        val pageHeight = getPageSize.second
        var fileDescriptor: ParcelFileDescriptor
        var pdfRenderer: PdfRenderer
        val document = PdfDocument()
        val fileName = Util.getCurrentTimeMillis()
        val filePath = "${storageDir}${fileName}.pdf"
        var thumbNail: Bitmap? = null
        var pageNum = 0
        var itemCount = 0
        var bitmap: Bitmap
        var currentPage: PdfRenderer.Page
        var pageInfo: PdfDocument.PageInfo
        var page: PdfDocument.Page
        var canvas: Canvas
        var xOffset: Float
        var yOffset: Float
        var canvasColor: Paint
        _completePercentage.postValue(0.0)


        data.forEachIndexed { _, file ->
            try {
                fileDescriptor = app.contentResolver.openFileDescriptor(file.uri, "r")!!
                if (fileDescriptor != null) {
                    pdfRenderer = PdfRenderer(fileDescriptor)
                    itemCount += pdfRenderer.pageCount

                    fileDescriptor.close()
                    pdfRenderer.close()
                }
            }catch (e: Exception){

            }
        }



        data.forEachIndexed { index, file ->

            val selectedPageInfo = pageInfoList?.items

            val pdfIndex: Int
            var selectedPagesList: List<Int> = emptyList()



            if (selectedPageInfo != null) {
                itemCount = 0
                val allItemCount =  selectedPageInfo.map { it.selectedPages.size }
                allItemCount.forEach { count -> itemCount += count }
                if (selectedPageInfo.any { it.pdfIndex == index && it.selectedPages.isNotEmpty() }) {
                    pdfIndex = index
                    selectedPagesList = selectedPageInfo[pdfIndex].selectedPages

                } else {
                    return@forEachIndexed
                }
            }

            var itemIncreasePercentage = (95.0 / itemCount)


            fileDescriptor = withContext(Dispatchers.IO) {
                app.contentResolver.openFileDescriptor(
                    file.uri,
                    "r"
                )!!
            }
            pdfRenderer = PdfRenderer(fileDescriptor)

            val pageCount = pdfRenderer.pageCount

            for (i in 0 until pageCount) {

                if (!selectedPagesList.contains(i)) {
                    continue
                }

                val job = scope.async {


                    currentPage = pdfRenderer.openPage(i)
                    bitmap = currentPage.let {
                        Bitmap.createBitmap(
                            pageWidth, pageHeight,
                            Bitmap.Config.ARGB_8888
                        )
                    }

                    bitmap.eraseColor(Color.WHITE)
                    Canvas(bitmap).drawBitmap(bitmap, 0f, 0f, null)

                    currentPage.render(
                        bitmap,
                        null,
                        null,
                        PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                    )

                    if (pageNum == 0) {
                        thumbNail = withContext(Dispatchers.Default) {
                            scaleBitmapToAspectRatio(
                                bitmap,
                                132,
                                132
                            )
                        }
                    }


                    pageInfo = PdfDocument.PageInfo
                        .Builder(pageWidth, pageHeight, pageNum)
                        .setContentRect(Rect(0, 0, pageWidth, pageHeight))
                        .create()
                    page = document.startPage(pageInfo)
                    canvas = page.canvas
                    xOffset = 0f  //((canvas.width - bitmap.width) / 2).toFloat()
                    yOffset = 0f //((canvas.height - bitmap.height) / 2).toFloat()
                    canvasColor = Paint(
                        ContextCompat.getColor(
                            app.applicationContext,
                            R.color.pdfBackground
                        )
                    )
                    canvas.drawBitmap(bitmap, xOffset, yOffset, canvasColor)
                    document.finishPage(page)


                    currentPage.close()
                    pageNum += 1
                    increasePercentage(itemIncreasePercentage)


                    _convertedProgressMessage.postValue("$pageNum of $itemCount files converted")
                    _convertedFileName.postValue("${file.fileName} Converting...")

                }
                job.join()
            }
            pdfRenderer.close()
            fileDescriptor.close()

        }

        try {

            var thumbNailUri: Uri? = null
            if (thumbNail != null) {
                withContext(Dispatchers.IO) {
                    thumbNailUri = savePdfThumbNail(thumbNail!!, fileName)
                }
            }

            val fos = withContext(Dispatchers.IO) {
                getOutputStream(filePath)
            }

            withContext(Dispatchers.IO) { writeDocument(document, fos) }
            val file = createConvertedPdfFile(fileName, filePath, thumbNailUri)

            viewModelScope.launch(Dispatchers.IO) {
                convertedFileList.add(file)
                repository.insertFile(file)
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            document.close()
        }


    }

    fun pdfIntoImage() {
        val data = this.data
        if (conversionPaused.value == true) {
            this.scope = CoroutineScope(SupervisorJob())
            _conversionPaused.postValue(false)
        }

        job = scope.launch {
            withContext(Dispatchers.Default) {
                convertAndSaveIntoImages(data, quality)
            }

        }.invokeOnCompletion { throwable ->
            if (throwable is CancellationException) {
                _conversionPaused.postValue(true)
                _convertedFileName.postValue("Conversion Stopped")
            } else {
                fillRemainingPercentage()
                _conversionFinished.postValue(true)
                _convertedProgressMessage.postValue("File Conversion Complete")
            }
        }

    }


    private suspend fun convertAndSaveIntoImages(
        data: List<ConvertInfo>,
        quality: Int
    ) {
        currentConvertedFiles.clear()
        val getPageSize = getPdfQuality(0)
        val pageWidth = getPageSize.first
        val pageHeight = getPageSize.second
        var fileDescriptor: ParcelFileDescriptor
        var pdfRenderer: PdfRenderer
        _completePercentage.postValue(0.0)
        var itemCount = 0
        var pageNum = 0

        data.forEachIndexed { _, file ->
            fileDescriptor = app.contentResolver.openFileDescriptor(file.uri, "r")!!
            pdfRenderer = PdfRenderer(fileDescriptor)
            itemCount += pdfRenderer.pageCount

            fileDescriptor.close()
            pdfRenderer.close()
        }


        data.forEachIndexed { index, file ->
            val rootFileName = Util.getCurrentTimeMillis()
            val selectedPageInfo = pageInfoList?.items
            var folderPath = ""
            val pdfIndex: Int
            var selectedPagesList: List<Int> = emptyList()

            if (selectedPageInfo != null) {
                itemCount = 0
                val allItemCount =  selectedPageInfo.map { it.selectedPages.size }
                allItemCount.forEach { count -> itemCount += count }
                if (selectedPageInfo.any { it.pdfIndex == index && it.selectedPages.isNotEmpty() }) {
                    pdfIndex = index
                    selectedPagesList = selectedPageInfo[pdfIndex].selectedPages
                    val folderName = File(file.filePath).nameWithoutExtension + "-$rootFileName"
                    folderPath =
                        createFileDirectory(folderName, itemCount)
                    this.imageFolderPath = folderPath
                } else {
                    return@forEachIndexed
                }
            }
            val itemIncreasePercentage = (95.0 / itemCount)


            var currentFilePageNum = 1

            fileDescriptor = withContext(Dispatchers.IO) {
                app.contentResolver.openFileDescriptor(file.uri, "r")!!
            }
            pdfRenderer = withContext(Dispatchers.IO) { PdfRenderer(fileDescriptor) }

            val pageCount = pdfRenderer.pageCount
            for (i in 0 until pageCount) {

                if (!selectedPagesList.contains(i)) {
                    continue
                }

                val job = scope.async {
                    val currentPage = pdfRenderer.openPage(i)

                    val bitmap = currentPage?.let { _ ->
                        Bitmap.createBitmap(
                            pageWidth, pageHeight,
                            Bitmap.Config.ARGB_8888
                        )
                    }


                    if (bitmap != null) {
                        bitmap.eraseColor(Color.WHITE)
                        val canvas = Canvas(bitmap)
                        canvas.drawBitmap(bitmap, 0f, 0f, null)

                        currentPage.render(
                            bitmap,
                            null,
                            null,
                            PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                        )

                        val convertToExtension = convertInto
                        val fileName = "$rootFileName-img-$currentFilePageNum"
                        val fileSavePath = getFileSavePath(folderPath, fileName, convertToExtension)
                        performConvert(bitmap, convertToExtension, quality, fileSavePath)
                        val currentImageFile = createConvertedImageFile(
                            fileSavePath,
                            convertToExtension
                        )
                        currentImageFile.apply {
                            inDirectory = true
                        }
                        saveConvertedFile(currentImageFile)
                        _convertedProgressMessage.postValue("$pageNum of $itemCount files converted")
                        _convertedFileName.postValue("$fileName Converting...")
                    }
                    pageNum += 1
                    currentFilePageNum += 1
                    currentPage.close()
                    increasePercentage(itemIncreasePercentage)
                }
                job.join()
            }

            pdfRenderer.close()
            fileDescriptor.close()
        }
    }


    fun pauseConversion() {
        scope.cancel()
    }

    private fun convertAndSaveFiles(item: ConvertInfo, storageDir: String, quality: Int) {
        val inputStream = app.contentResolver.openInputStream(item.uri)

        try {
            val options = BitmapFactory.Options()
            options.inPreferredConfig = Bitmap.Config.ARGB_8888
            options.inScaled = false
            val inputBitmap = BitmapFactory.decodeStream(inputStream, null, options)
            Log.d("Spinnervalues", fileQuality.toString())
            val getImageSize = inputBitmap?.let { getImageSize(it, fileQuality) }
            val bitmap =
                inputBitmap?.let { getImageSize?.let { it1 -> scaleImage(it, it1.first, getImageSize.second) } }
            val convertToExtension = convertInto
            val fileName = getFileName(item.fileName) + "-" + Util.getCurrentTimeMillis()
            val fileSavePath = getFileSavePath(storageDir, fileName, convertToExtension)
            if (bitmap != null) {
                performConvert(bitmap, convertToExtension, quality, fileSavePath)
                val file = createConvertedImageFile(
                    fileSavePath,
                    convertToExtension
                )
                saveConvertedFile(file)
            }




            inputStream?.close()
        }catch (e: FileNotFoundException) {
            Toast.makeText(app,"File Not Found", Toast.LENGTH_SHORT).show()
        }
        catch (e: Exception) {
            e.printStackTrace()
        } finally {
            inputStream?.close()
        }
    }

    private fun saveConvertedFile(file: ConvertedFile) {
        if (file.fileType != "pdf") {
            scope.launch {
                ensureActive()
                convertedFileList.add(file)
                repository.insertFile(file)
            }
        }
    }

    private fun performConvert(bitmap: Bitmap, to: String, quality: Int, filePath: String) {
        when (to) {
            ".jpg", ".jpeg" -> {

                val fos = FileOutputStream(filePath)
                try {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, fos)
                } catch (e: java.lang.Exception) {

                } finally {

                    fos.close()
                }
            }
            ".png" -> {

                val fos = FileOutputStream(filePath)
                try {
                    Log.d("imgQuality", quality.toString())
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                } catch (e: java.lang.Exception) {

                } finally {

                    fos.close()
                }

            }
            ".webp" -> {
                val fos = FileOutputStream(filePath)
                try {
                    bitmap.compress(Bitmap.CompressFormat.WEBP, quality, fos)
                } catch (e: java.lang.Exception) {
                } finally {

                    fos.close()
                }
            }
            ".pdf" -> {
                if (scope.isActive) {
                    createAndSaveSinglePagePdf(bitmap, this.storageDir)
                }
            }


        }
    }

    private fun createAndSaveSinglePagePdf(inputBitmap: Bitmap, storageDir: String) {
        val pageSize = getPageSize(fileQuality, pdfPageSize)
        val pageWidth = pageSize.first
        val pageHeight = pageSize.second
        val bitmap = scaleBitmapToAspectRatio(inputBitmap, pageWidth, pageHeight)
        val fileName = Util.getCurrentTimeMillis()
        val filePath = "${storageDir}${fileName}.pdf"
        val document = PdfDocument()
        val pageInfo: PdfDocument.PageInfo =
            PdfDocument.PageInfo.Builder(pageWidth + padding, pageHeight + padding, 1)
                .setContentRect(Rect(0, 0, pageWidth + padding, pageHeight + padding)).create()
        val page: PdfDocument.Page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val xOffset = ((canvas.width - bitmap.width) / 2).toFloat()
        val yOffset = ((canvas.height - bitmap.height) / 2).toFloat()
        canvas.drawBitmap(bitmap, xOffset, yOffset, null)
        document.finishPage(page)
        val thumbNail: Bitmap = scaleBitmapToAspectRatio(inputBitmap, 132, 132)


        val fos = getOutputStream(filePath)
        try {
            writeDocument(document, fos)
            val thumbNailUri: Uri? = savePdfThumbNail(thumbNail, fileName)

            val file = createConvertedPdfFile(fileName, filePath, thumbNailUri)


            viewModelScope.launch {
                convertedFileList.add(file)
                repository.insertFile(file)

            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            fos.close()
            document.close()
        }
    }

    private suspend fun createAndSaveMultiPagePdf(items: List<ConvertInfo>, storageDir: String) {
        itemPercentage = (95 / items.size).toDouble()
        val fileName = Util.getCurrentTimeMillis()
        val filePath = "${storageDir}${fileName}.pdf"
        val document = PdfDocument()
        var thumbNail: Bitmap? = null
        val pageSize = getPageSize(fileQuality, pdfPageSize)
        val pageWidth = pageSize.first
        val pageHeight = pageSize.second


        items.forEachIndexed { index, item ->
            withContext(Dispatchers.IO) {
                val inputStream = app.contentResolver.openInputStream(item.uri)
                try {
                    var bitmap =
                        withContext(Dispatchers.IO) { BitmapFactory.decodeStream(inputStream) }
                    if (index == 0) {
                        thumbNail = withContext(Dispatchers.Default) {
                            scaleBitmapToAspectRatio(
                                bitmap,
                                132,
                                132
                            )
                        }
                    }

                    bitmap = scaleBitmapToAspectRatio(
                        bitmap,
                        pageWidth,
                        pageHeight
                    )



                    val pageInfo: PdfDocument.PageInfo = PdfDocument.PageInfo
                        .Builder(pageWidth + padding, pageHeight + padding, index + 1)
                        .setContentRect(Rect(0, 0, pageWidth + padding, pageHeight + padding)).create()
                    val page: PdfDocument.Page = document.startPage(pageInfo)
                    val canvas: Canvas = page.canvas
                    val xOffset = ((canvas.width - bitmap.width) / 2).toFloat()
                    val yOffset = ((canvas.height - bitmap.height) / 2).toFloat()
                    val canvasColor =
                        Paint(
                            ContextCompat.getColor(
                                app.applicationContext,
                                R.color.pdfBackground
                            )
                        )
                    canvas.drawBitmap(bitmap, xOffset, yOffset, canvasColor)
                    document.finishPage(page)
                    _convertedFileName.postValue("${item.fileName}:Converted")
                    increasePercentage(itemPercentage)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    inputStream?.close()
                }
            }
        }



        try {

            var thumbNailUri: Uri? = null
            if (thumbNail != null) {
                withContext(Dispatchers.IO) {
                    thumbNailUri = savePdfThumbNail(thumbNail!!, fileName)
                }
            }

            val fos = withContext(Dispatchers.IO) {
                getOutputStream(filePath)
            }

            withContext(Dispatchers.IO) { writeDocument(document, fos) }
            val file = createConvertedPdfFile(fileName, filePath, thumbNailUri)

            viewModelScope.launch(Dispatchers.IO) {
                convertedFileList.add(file)
                repository.insertFile(file)
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            document.close()
        }
    }

    private fun savePdfThumbNail(bitmap: Bitmap, fileName: String): Uri? {
        val folder = File(storageDir, ".PDF")
        if (!folder.exists()) {
            folder.mkdir()
        }

        val file = File(folder, "$fileName.png")
        var fos: FileOutputStream? = null

        try {
            fos = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.close()
            return file.toUri()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            fos?.close()
        }
        return null
    }

    private fun getOutputStream(filePath: String): FileOutputStream {
        _convertedFileName.postValue("Creating file...")
        val fos: FileOutputStream?
        fos = FileOutputStream(filePath)
        return fos
    }

    private fun writeDocument(doc: PdfDocument, fos: FileOutputStream) {
        _convertedFileName.postValue("Writing data into file(s)...")
        doc.writeTo(fos)
    }

    private fun scaleImage(targetBmp: Bitmap, reqWidthInPixels: Int, reqHeightInPixels: Int): Bitmap? {
        return Bitmap.createScaledBitmap(targetBmp, reqWidthInPixels, reqHeightInPixels, false)
    }

    private fun scaleBitmapToAspectRatio(
        targetBmp: Bitmap,
        reqWidthInPixels: Int,
        reqHeightInPixels: Int
    ): Bitmap {
        val matrix = Matrix()
        matrix.setRectToRect(
            RectF(0F, 0F, targetBmp.width.toFloat(), targetBmp.height.toFloat()),
            RectF(0F, 0F, reqWidthInPixels.toFloat(), reqHeightInPixels.toFloat()),
            Matrix.ScaleToFit.CENTER
        )
        return Bitmap.createBitmap(
            targetBmp,
            0,
            0,
            targetBmp.width,
            targetBmp.height,
            matrix,
            false
        )
    }

    private fun getPdfQuality(value: Int): Pair<Int, Int> {
        when (value) {
            0 -> {
                return Pair(595, 842)
            }
            1 -> {
                return Pair(1654, 2339)
            }
            2 -> {
                return Pair(2480, 3508)
            }
            3 -> {
                return Pair(3307, 4677)
            }
            else -> {
                return Pair(595, 842)
            }
        }
    }

    private fun getPageSize(fileQuality: Int, pageQuality: Int): Pair<Int, Int> {
        when(fileQuality) {
            0 -> {
               when(pageQuality) {
                   0 -> {
                       return Pair(2384, 3370)
                   }
                   1 -> {
                       return Pair(1684, 2384)
                   }
                   2 -> {
                       return Pair(1191, 1684)
                   }
                   3 -> {
                       return Pair(842, 1191)
                   }
                   4 -> {
                       return Pair(595, 842)
                   }
                   5 -> {
                       return Pair(420, 595)
                   }
                   else -> {
                       return Pair(0, 0)
                   }
               }
            }
            1 -> {
                when(pageQuality) {
                    0 -> {
                        return Pair(3179, 4494)
                    }
                    1 -> {
                        return Pair(2245, 3179)
                    }
                    2 -> {
                        return Pair(1587, 2245)
                    }
                    3 -> {
                        return Pair(1123, 1587)
                    }
                    4 -> {
                        return Pair(794, 1123)
                    }
                    5 -> {
                        return Pair(559, 794)
                    }
                    else -> {
                        return Pair(0, 0)
                    }
                }
            }
            2 -> {
                when(pageQuality) {
                    0 -> {
                        return Pair(4967, 7022)
                    }
                    1 -> {
                        return Pair(3508, 4967)
                    }
                    2 -> {
                        return Pair(2480, 3508)
                    }
                    3 -> {
                        return Pair(1754, 2480)
                    }
                    4 -> {
                        return Pair(1240, 1754)
                    }
                    5 -> {
                        return Pair(874, 1240)
                    }
                    else -> {
                        return Pair(0, 0)
                    }
                }
            }
            3 -> {
                when(pageQuality) {
                    0 -> {
                        return Pair(9933, 14043)
                    }
                    1 -> {
                        return Pair(7016, 9933)
                    }
                    2 -> {
                        return Pair(4960, 7016)
                    }
                    3 -> {
                        return Pair(3508, 4960)
                    }
                    4 -> {
                        return Pair(2480, 3508)
                    }
                    5 -> {
                        return Pair(1748, 2480)
                    }
                    else -> {
                        return Pair(0, 0)
                    }
                }
            }
            else -> {
                return Pair(0, 0)
            }

        }
    }

    private fun getImageSize(bitmap: Bitmap,value: Int): Pair<Int,Int> {
        when (value) {
            0 -> {
                return Pair(bitmap.width, bitmap.height)
            }
            1 -> {
                return Pair(320, 320)
            }
            2 -> {
                return Pair(1080, 566)
            }
            3 -> {
                return Pair(1080, 1350)
            }
            4 -> {
                return Pair(1080, 1080)
            }
            5 -> {
                return Pair(1080, 1920)
            }
            6 -> {
                return Pair(400, 400)
            }
            7 -> {
                return Pair(1500, 500)
            }
            8 -> {
                return Pair(1600, 1900)
            }
            9 -> {
                return Pair(170, 170)
            }
            10 -> {
                return Pair(851 , 315)
            }
            11 -> {
                return Pair(1200, 630)
            }
            12 -> {
                return Pair(1200, 628)
            }
            13 -> {
                return Pair(1000, 1500)
            }
            14 -> {
                return Pair(1000, 1000)
            }
            15 -> {
                return Pair(1000, 2100)
            }
            16 -> {
                return Pair(1000, 3000)
            }
            else -> {
                return Pair(0,0)
            }
        }
    }

    private fun getFileName(fileName: String): String {
        return File(fileName).nameWithoutExtension
    }

    private fun getFileSavePath(
        storageDir: String,
        fileName: String,
        fileExtension: String
    ): String {
        return Util.getStoragePathWithExtension(storageDir, fileName, fileExtension)
    }

    private fun getExternalDir(): String {
        return Util.getExternalDir(app.applicationContext)
    }

    private fun createFileDirectory(name: String, fileCount: Int): String {
        try {

            val dirPath = Util.getExternalDir(app.applicationContext)
            val folderPath = Util.getStorageFolder(dirPath, name)
            val contentSize = Util.getContentSize(fileCount)
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
                inDirectory = false,
                directoryColor,
                date
            )
            viewModelScope.launch {
                repository.insertFile(folder)
            }

            return if (file.exists()) "$folderPath/" else ""
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return ""
    }

    private fun fillRemainingPercentage() {
        _completePercentage.postValue(100.0)
    }

    private fun createConvertedImageFile(filePath: String, extension: String): ConvertedFile {
        val millis = Util.getCurrentTimeMillis()
        val fileName = File(filePath).name
        val uri = File(filePath).toUri()
        val date = Date(millis.toLong())
        val fileSize = File(filePath).length()
        val fileSizeString = Util.convertBytes(fileSize)
        return ConvertedFile(
            0,
            fileName,
            fileSizeString,
            fileSize,
            filePath,
            extension.substring(1),
            uri,
            null,
            isFavorite = false,
            isSelected = false,
            isDirectory = false,
            inDirectory = false,
            null,
            date
        )
    }

    private fun createConvertedPdfFile(
        fileName: String,
        filePath: String,
        thumbNailUri: Uri?
    ): ConvertedFile {
        val millis = Util.getCurrentTimeMillis()
        val uri = File(filePath).toUri()
        val date = Date(millis.toLong())
        val fileSize = File(filePath).length()
        val fileSizeString = Util.convertBytes(fileSize)
        return ConvertedFile(
            0,
            "$fileName.pdf",
            fileSizeString,
            fileSize,
            filePath,
            "pdf",
            uri,
            thumbNailUri,
            isFavorite = false,
            isSelected = false,
            isDirectory = false,
            inDirectory = false,
            directoryColor = null,
            date
        )
    }

    private fun increasePercentage(value: Double = 1.0) {
        _completePercentage.postValue(completePercentage.value?.plus(value))
    }

    fun setConversionPaused(value: Boolean) {
        _conversionPaused.postValue(value)
    }


    override fun onCleared() {
        super.onCleared()
        _completePercentage.postValue(0.0)
        scope.cancel()
    }

}