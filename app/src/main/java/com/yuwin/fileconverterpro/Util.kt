package com.yuwin.fileconverterpro

import android.content.Context
import android.database.Cursor
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.ContextCompat
import java.io.File
import java.lang.String.format
import java.text.CharacterIterator
import java.text.SimpleDateFormat
import java.text.StringCharacterIterator
import java.util.*
import kotlin.math.abs

class Util {

    companion object {

        fun convertBytes(bytes: Long): String {
            val absB = if (bytes == Long.MIN_VALUE) Long.MAX_VALUE else abs(bytes)
            if (absB < 1024) {
                return "$bytes B"
            }
            var value = absB
            val ci: CharacterIterator = StringCharacterIterator("KMGTPE")
            var i = 40
            while (i >= 0 && absB > 0xfffccccccccccccL shr i) {
                value = value shr 10
                ci.next()
                i -= 10
            }
            value *= java.lang.Long.signum(bytes).toLong()
            return format("%.1f %cB", value / 1024.0, ci.current())
        }

        fun getCurrentTimeMillis(): String {
            return System.currentTimeMillis().toString()
        }

        fun getDateString(milliSeconds: Long): String {
            val formatter = SimpleDateFormat("dd MMM yyyy hh:mm", Locale.getDefault())
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = milliSeconds
            return formatter.format(calendar.time)
        }

        fun getCurrentDateTime(milliSeconds: Long): Date {
            return Date(milliSeconds)
        }

        fun getMimeType(context: Context, uri: Uri): String? {
            val cr  = context.contentResolver
            val mime = MimeTypeMap.getSingleton()
            return mime.getExtensionFromMimeType(cr.getType(uri))?.toUpperCase(Locale.ROOT)
        }

        fun getImageDetails(context: Context, uri: Uri): FileInfo {
            val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)

            cursor?.let {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                it.moveToFirst()

                val fileName =  it.getString(nameIndex)
                val fileSize =  convertBytes(it.getLong(sizeIndex)).toString()
                cursor.close()

                return FileInfo(fileName, fileSize)
            }
            return FileInfo("N/A", "N/A")
        }

        fun getFileExtension(specificFormat: Int?, defaultFormat: Int?, convertAll: Boolean?): String {
            return if(convertAll == true) {
                ".${FormatTypes.values()[defaultFormat!!].toString().toLowerCase(Locale.ROOT)}"
            }else {
                ".${FormatTypes.values()[specificFormat!!].toString().toLowerCase(Locale.ROOT)}"
            }
        }



        // Storage Details
        fun getExternalDir(context: Context): String {
            val externalStorageVolumes: Array<out File> =
                    ContextCompat.getExternalFilesDirs(context.applicationContext, null)
            return externalStorageVolumes[0].absolutePath + "/"
        }

        fun getStoragePath(storageDir: String, fileName: String, fileExtension: String): String {
            return "$storageDir$fileName$fileExtension"
        }

        fun isExternalStorageWritable(): Boolean {
            return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
        }

        fun isExternalStorageReadable(): Boolean {
            return Environment.getExternalStorageState() in
                    setOf(Environment.MEDIA_MOUNTED, Environment.MEDIA_MOUNTED_READ_ONLY)
        }



    }
}