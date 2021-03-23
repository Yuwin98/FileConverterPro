package com.yuwin.fileconverterpro

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.lifecycle.*
import kotlinx.coroutines.launch

class PdfPreviewViewModel(val app: Application) : AndroidViewModel(app) {

    private var currentPage: PdfRenderer.Page? = null

    private lateinit var pdfRenderer: PdfRenderer

    private lateinit var fileDescriptor: ParcelFileDescriptor

    private val _progressValue = MutableLiveData<Int>().apply { value = 0 }
    val progressValue: LiveData<Int> get() = _progressValue

    private val _progressText = MutableLiveData<String>()
    val progressText: LiveData<String> get() = _progressText

    var pageCount: Int = 0

    var isAllSelected = false

    private val _data = MutableLiveData<MutableList<PdfPreviewModel>?>()
    val data: MutableLiveData<MutableList<PdfPreviewModel>?> get() =  _data

    private val _selectedPages = MutableLiveData<MutableList<Int>>()
    val selectedPage: LiveData<MutableList<Int>> get() = _selectedPages



    init {
        _selectedPages.value = mutableListOf()
        _data.value = mutableListOf()
        _progressText.postValue("")
    }

    private fun incrementProgressValue() {
        _progressValue.value?.let { a ->
            _progressValue.value = a + 1
        }
    }

    private fun changeProgressText(value: Int) {
        _progressText.postValue("Opening ${progressValue.value} of $value")
    }


     fun openPdfRenderer(fileUri: Uri) {
        fileDescriptor =
            app.applicationContext.contentResolver.openFileDescriptor(fileUri, "r")!!
        pdfRenderer = PdfRenderer(fileDescriptor)
        pageCount = pdfRenderer.pageCount
    }

     fun closePdfRenderer() {
        currentPage?.close()
        pdfRenderer.close()
        fileDescriptor.close()

    }

     fun createPageBitmapList() {

        for (i in 0 until pageCount) {
            currentPage?.close()
            currentPage = pdfRenderer.openPage(i)
            val bitmap = currentPage?.let {
                Bitmap.createBitmap(
                    it.width, it.height,
                    Bitmap.Config.ARGB_8888
                )
            }

            if (bitmap != null) {
                bitmap.eraseColor(Color.WHITE)
                Canvas(bitmap).drawBitmap(bitmap, 0f, 0f, null)
                currentPage?.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                viewModelScope.launch {
                    incrementProgressValue()
                    changeProgressText(pageCount)
                }
                val dataItem = PdfPreviewModel(bitmap, false)
                _data.value?.add(dataItem)
            }

        }
    }

    fun updateDataList(newData: MutableList<PdfPreviewModel>) {
        _data.value = newData
    }

    fun updateSelectedPages(selectedPageList: MutableList<Int>) {
        _selectedPages.value = selectedPageList
    }

    override fun onCleared() {
        super.onCleared()
        _data.postValue(null)
    }


}