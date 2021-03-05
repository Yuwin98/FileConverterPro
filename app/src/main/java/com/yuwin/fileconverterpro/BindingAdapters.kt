package com.yuwin.fileconverterpro

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
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


    }
}