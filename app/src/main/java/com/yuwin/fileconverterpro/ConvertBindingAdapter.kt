package com.yuwin.fileconverterpro

import android.net.Uri
import android.view.View
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import com.bumptech.glide.Glide
import com.yuwin.fileconverterpro.db.ConvertedFile
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class ConvertBindingAdapter {

    companion object {

        @BindingAdapter("loadThumbnailFromUri")
        @JvmStatic
        fun loadThumbnailImage(view: ImageView, convertedFile: ConvertedFile) {
            if(convertedFile.fileType == "pdf"){
                Glide.with(view).load(convertedFile.thumbnailUri).into(view)
            }else{
                Glide.with(view).load(convertedFile.uri).into(view)
            }
        }

        @BindingAdapter("loadImageFromUri")
        @JvmStatic
        fun loadImage(view: ImageView, uri:Uri) {
                Glide.with(view).load(uri).into(view)

        }

        @BindingAdapter("getDefaultConvertFormat")
        @JvmStatic
        fun getDefaultConvertFormat(view: TextView, ordinal: Int) {
            view.text = FormatTypes.values()[ordinal].toString()
        }

        @BindingAdapter("getDateString")
        @JvmStatic
        fun getDateString(view: TextView, date: Date) {
            val sdf = SimpleDateFormat("d MMM yyyy HH:mm", Locale.getDefault())
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
                view.text ="$begin$mid$end"

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
                view.text ="$begin$mid$end"

            }else {
                view.text = fileName
            }
        }




    }
}