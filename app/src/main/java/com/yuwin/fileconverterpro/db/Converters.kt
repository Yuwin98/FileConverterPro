package com.yuwin.fileconverterpro.db

import android.net.Uri
import androidx.room.TypeConverter
import com.yuwin.fileconverterpro.Util
import java.text.SimpleDateFormat
import java.util.*

class Converters {

    @TypeConverter
    fun uriToString(uri: Uri?): String {
        if(uri == null) {
            return ""
        }
        return uri.toString()
    }

    @TypeConverter
    fun stringToUri(uri: String?): Uri? {
        if(uri == null) {
            return Uri.EMPTY
        }
        return Uri.parse(uri)
    }

    @TypeConverter
    fun dateToString(date: Date): String {
        val df = SimpleDateFormat("d MMM yyyy HH:mm", Locale.getDefault())
        return df.format(date.time)
    }

    @TypeConverter
    fun stringToDate(date: String): Date? {
        val sdf = SimpleDateFormat("dd MMM yyyy hh:mm", Locale.getDefault())
        return  sdf.parse(date)
    }



}