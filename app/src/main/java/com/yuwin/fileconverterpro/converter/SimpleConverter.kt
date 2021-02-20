package com.yuwin.fileconverterpro.converter

import android.graphics.Color
import com.yuwin.fileconverterpro.FormatTypes
import java.io.File

interface SimpleConverter {

    fun convert(file: File, from: FormatTypes, to: FormatTypes)

    fun convertPng(file: File, from: FormatTypes, to: FormatTypes, color: Color)


}