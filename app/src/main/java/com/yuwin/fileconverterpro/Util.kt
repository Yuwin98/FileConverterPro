package com.yuwin.fileconverterpro

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import java.lang.String.format
import java.text.CharacterIterator
import java.text.StringCharacterIterator
import java.util.*
import kotlin.math.abs

class Util {

    companion object {

        fun convertBytes(bytes: Long): String? {
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

        fun getMimeType(context: Context, uri: Uri): String? {
            val cr  = context.contentResolver
            val mime = MimeTypeMap.getSingleton()
            return mime.getExtensionFromMimeType(cr.getType(uri))?.toUpperCase(Locale.ROOT)
        }

        fun getImageDetails(context: Context ,uri: Uri): FileInfo {
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


    }
}