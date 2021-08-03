package com.yuwin.fileconverterpro

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.lifecycle.*
import kotlinx.coroutines.launch
import java.io.FileNotFoundException
import java.lang.Exception

class PdfPreviewViewModel(val app: Application) : AndroidViewModel(app) {

    private var currentPage: PdfRenderer.Page? = null

    private  var pdfRenderer: PdfRenderer? = null

    private  var fileDescriptor: ParcelFileDescriptor? = null

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
         try {
             fileDescriptor =
                 app.applicationContext.contentResolver.openFileDescriptor(fileUri, "r")!!
             fileDescriptor?.let { pfd ->
                 pdfRenderer = PdfRenderer(pfd)
                 pdfRenderer?.let {
                     pageCount = it.pageCount
                 }
             }


         }catch (e: FileNotFoundException) {
            Toast.makeText(app, "File Not found", Toast.LENGTH_SHORT).show()
         }catch (e: Exception) {
             e.printStackTrace()
         }

    }

     private fun closePdfRenderer() {
        if(currentPage != null) {
            currentPage?.close()
        }
         pdfRenderer?.close()
         fileDescriptor?.close()

    }

     fun createPageBitmapList() {

        for (i in 0 until pageCount) {
            try {
                currentPage?.close()
                currentPage = pdfRenderer?.openPage(i)
                val bitmap = currentPage?.let {

                    Bitmap.createBitmap(
                        Util.dipToPixels(app, 140f).toInt(), Util.dipToPixels(app, 140f).toInt(),
                        Bitmap.Config.ARGB_8888
                    )
                }

                if (bitmap != null) {
                    bitmap.eraseColor(Color.WHITE)
                    Canvas(bitmap).drawBitmap(bitmap, 0f, 0f, null)
                    currentPage?.render(
                        bitmap,
                        null,
                        null,
                        PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                    )
                    viewModelScope.launch {
                        incrementProgressValue()
                        changeProgressText(pageCount)
                    }
                    val dataItem = PdfPreviewModel(bitmap, false)
                    _data.value?.add(dataItem)
                }
            }catch (e: Exception) {
                continue
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
        closePdfRenderer()
        _data.postValue(null)
    }


}