package com.yuwin.fileconverterpro

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.yuwin.fileconverterpro.db.ConvertedFile
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class BindingAdapters {

    companion object {

        @BindingAdapter("loadThumbnailFromUri")
        @JvmStatic
        fun loadThumbnailImage(view: ImageView, convertedFile: ConvertedFile) {
            when (convertedFile.fileType) {
                "pdf" -> {
                    Glide.with(view).load(convertedFile.thumbnailUri).into(view)
                }
                "Directory" -> {
                    if(view.id == R.id.fileListGridImageView) {
                        val dims = TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            56f,
                            view.resources.displayMetrics
                        )
                        view.layoutParams.height = dims.toInt()
                        view.layoutParams.width = dims.toInt()
                    }
                    Glide.with(view).load(R.drawable.ic_folder_black_).into(view)
                    val folderColorList = Util.getAllFolderColors(view.context)
                    val folderColor = folderColorList[convertedFile.directoryColor!!]
                    ImageViewCompat.setImageTintList(view, ColorStateList.valueOf(folderColor))
                }
                else -> {
                    Glide.with(view).load(convertedFile.uri).into(view)
                }
            }
        }


        @BindingAdapter("loadImageFromUri")
        @JvmStatic
        fun loadImage(view: ImageView, uri: Uri) {
            Glide.with(view).load(uri).into(view)
        }

        @BindingAdapter("loadImagePDFThumbnail")
        @JvmStatic
        fun loadImagePDFThumbnail(view: ImageView, item: ConvertInfo) {
            if(item.fileType != "PDF") {
                Glide.with(view).load(item.uri).into(view)
            }else {
               val bitmap = loadPDFThumbnail(view.context,item)
               Glide.with(view).load(bitmap).into(view)
            }
        }

        private fun loadPDFThumbnail(context: Context,item: ConvertInfo): Bitmap? {
            val fileDescriptor = context.contentResolver.openFileDescriptor(
                item.uri,
                "r"
            )!!
            val renderer  = PdfRenderer(fileDescriptor)
            val page = renderer.openPage(0)
            var bitmap = page?.let {
                Bitmap.createBitmap(
                    it.width,
                    it.height,
                    Bitmap.Config.ARGB_8888
                )
            }
            if (bitmap != null) {
                bitmap.eraseColor(Color.WHITE)
                Canvas(bitmap).drawBitmap(bitmap, 0f, 0f, null)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            }
            bitmap = bitmap?.let { Util.scaleBitmap(it, 256 * 256) }

            return bitmap
        }

        @BindingAdapter("getDefaultConvertFormat")
        @JvmStatic
        fun getDefaultConvertFormat(view: TextView, item: ConvertInfo) {
            if(item.isPdfConversion == true) {
                view.text = FormatTypesPDF.values()[item.defaultConvertFormat!!].toString()
            }else {
                view.text = FormatTypes.values()[item.defaultConvertFormat!!].toString()
            }
        }

        @BindingAdapter("getDateString")
        @JvmStatic
        fun getDateString(view: TextView, date: Date) {
            val sdf = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
            view.text = sdf.format(date)
        }

        @BindingAdapter("getFileSize")
        @JvmStatic
        fun getFileSize(view: TextView, filePath: String ) {
            val file = File(filePath)
            val size = file.length()
            view.text = Util.convertBytes(size)
        }

        @BindingAdapter("fileNameListAdapter")
        @JvmStatic
        fun fileNameListAdapter(view: TextView, fileName: String) {
            if(fileName.length > 30) {
                val begin = fileName.take(13)
                val mid = "..."
                val end = fileName.takeLast(13)
                val compatFileName = "$begin$mid$end"
                view.text = compatFileName

            }else {
                view.text = fileName
            }
        }

        @BindingAdapter("fileNameGridAdapter")
        @JvmStatic
        fun fileNameGridAdapter(view: TextView, fileName: String) {
            if(fileName.length > 16) {
                val begin = fileName.take(6)
                val mid = "..."
                val end = fileName.takeLast(6)
                val compatFileName = "$begin$mid$end"
                view.text = compatFileName

            }else {
                view.text = fileName
            }
        }


        @BindingAdapter("changeFavoriteIcon")
        @JvmStatic
        fun changeFavoriteIcon(view: ImageView, isFavorite: Boolean) {
            if(isFavorite) {
                view.setImageDrawable(
                    ContextCompat.getDrawable(view.context, R.drawable.ic_favorite)
                )
            }else {
                view.setImageDrawable(
                    ContextCompat.getDrawable(view.context, R.drawable.ic_favorite_border)
                )
            }
        }

        @BindingAdapter("changeSelectedListItemColor")
        @JvmStatic
        fun changeSelectedListItemColor(view: ConstraintLayout, isSelected: Boolean) {
            if(isSelected) {
                view.setBackgroundColor(
                        ContextCompat.getColor(
                                view.context,
                                R.color.selectedFileItemBackground
                                )
                )
            }else {
                view.setBackgroundColor(
                        ContextCompat.getColor(
                                view.context,
                                R.color.colorBackground
                        )
                )
            }
        }

        @BindingAdapter("changeSelectedGridItemColor")
        @JvmStatic
        fun changeSelectedGridItemColor(view: ConstraintLayout, isSelected: Boolean) {
            if(isSelected) {
                view.foreground = ColorDrawable(
                        ContextCompat.getColor(view.context, R.color.selectedFileItemBackground)
                )
            }else {
                view.foreground = ColorDrawable( Color.argb(0,0,0,0)
                )
            }
        }

        @BindingAdapter("changeNonFolderColor")
        @JvmStatic
        fun changeNonFolderColor(view: ImageView, isDirectory: Boolean) {
            if(!isDirectory) {
                ImageViewCompat.setImageTintList(view, null)
            }
        }

        @BindingAdapter("getFileInfo")
        @JvmStatic
        fun getFileInfo(view: TextView, file: ConvertedFile) {
            if(file.isDirectory) {
                val files = File(file.filePath).listFiles()
                if(files != null) {
                    val fileSize = Util.getContentSize(files.size)
                    view.text = fileSize
                }
            }else {
                view.text = file.fileSize
            }
        }


        @BindingAdapter("spinnerVisibility")
        @JvmStatic
        fun spinnerVisibility(view: Spinner, item: ConvertInfo) {
            if(item.fileType == "PDF" && item.convertAll == false) {
                Log.d("mimeTypePDF", "A pdf conversion")
                when(view.id) {
                    R.id.fileTypeSpinner -> {
                        view.visibility  = View.GONE
                    }
                    R.id.pdfTypeSpinner -> {
                        view.visibility = View.VISIBLE
                    }
                }

            }else if(item.convertAll == false) {
                Log.d("mimeTypePDF", "Image conversion")

                when(view.id) {
                    R.id.fileTypeSpinner -> {
                        view.visibility  = View.VISIBLE
                    }
                    R.id.pdfTypeSpinner -> {
                        view.visibility = View.GONE
                    }
                }
            }else {
                when(view.id) {
                    R.id.fileTypeSpinner -> {
                        view.visibility  = View.GONE
                    }
                    R.id.pdfTypeSpinner -> {
                        view.visibility = View.GONE
                    }
                }
            }
        }


    }
}