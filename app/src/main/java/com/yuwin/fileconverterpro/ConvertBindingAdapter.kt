package com.yuwin.fileconverterpro

import android.net.Uri
import android.view.View
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import com.bumptech.glide.Glide


class ConvertBindingAdapter {

    companion object {

        @BindingAdapter("loadImageFromUri")
        @JvmStatic
        fun loadImage(view: ImageView, uri: Uri) {
            Glide.with(view).load(uri).into(view)
        }

        @BindingAdapter("getDefaultConvertFormat")
        @JvmStatic
        fun getDefaultConvertFormat(view: TextView, ordinal: Int) {
            view.text = FormatTypes.values()[ordinal].toString()
        }




    }
}