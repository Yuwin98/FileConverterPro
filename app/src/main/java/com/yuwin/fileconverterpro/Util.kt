package com.yuwin.fileconverterpro

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.yuwin.fileconverterpro.db.ConvertedFile
import java.io.File
import java.lang.String.format
import java.text.CharacterIterator
import java.text.SimpleDateFormat
import java.text.StringCharacterIterator
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sqrt


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

        fun retrieveFileSize(file: File): Long {
            var fileSize = 0L

            if (file.isDirectory) {
                val files = file.listFiles()
                files?.forEach { currentFile ->
                    val currentFileSize = retrieveFileSize(currentFile)
                    fileSize += currentFileSize
                }
            } else {
                fileSize += file.length()
            }

            return fileSize
        }

        fun getCurrentTimeMillis(): String {
            return System.currentTimeMillis().toString()
        }

        fun getMimeType(context: Context, uri: Uri): String? {
            val cr = context.contentResolver
            val mime = MimeTypeMap.getSingleton()
            return mime.getExtensionFromMimeType(cr.getType(uri))?.toUpperCase(Locale.ROOT)
        }

        fun getImageDetails(context: Context, uri: Uri): FileInfo {
            val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)

            cursor?.let {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                it.moveToFirst()

                val fileName = it.getString(nameIndex)
                val fileSize = convertBytes(it.getLong(sizeIndex))
                cursor.close()

                return FileInfo(fileName, fileSize)
            }
            return FileInfo("N/A", "N/A")
        }

        fun getFileExtension(
            specificFormat: Int?,
            defaultFormat: Int?,
            convertAll: Boolean?,
            isPdfConversion: Boolean?
        ): String {
            return if (isPdfConversion == true) {
                if (convertAll == true) {
                    ".${
                        FormatTypesPDF.values()[defaultFormat!!].toString().toLowerCase(Locale.ROOT)
                    }"
                } else {
                    ".${
                        FormatTypesPDF.values()[specificFormat!!].toString()
                            .toLowerCase(Locale.ROOT)
                    }"
                }
            } else {
                if (convertAll == true) {
                    ".${FormatTypes.values()[defaultFormat!!].toString().toLowerCase(Locale.ROOT)}"
                } else {
                    ".${FormatTypes.values()[specificFormat!!].toString().toLowerCase(Locale.ROOT)}"
                }
            }
        }


        // Storage Details
        fun getExternalDir(context: Context): String {
            val externalStorageVolumes: Array<out File> =
                ContextCompat.getExternalFilesDirs(context.applicationContext, null)
            return externalStorageVolumes[0].absolutePath + "/"
        }


        fun getStoragePathWithExtension(
            storageDir: String,
            fileName: String,
            fileExtension: String
        ): String {
            return "$storageDir$fileName$fileExtension"
        }

        fun getStorageFolder(storageDir: String, folderName: String): String {
            return "$storageDir$folderName"
        }

        fun getContentSize(value: Int): String {
            return when (value) {
                0 -> "0 items"
                1 -> "1 item"
                else -> "$value items"
            }

        }

        fun isExternalStorageWritable(): Boolean {
            return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
        }

        fun isExternalStorageReadable(): Boolean {
            return Environment.getExternalStorageState() in
                    setOf(Environment.MEDIA_MOUNTED, Environment.MEDIA_MOUNTED_READ_ONLY)
        }

        fun getSendingType(context: Context, file: ConvertedFile): String {

            when (getMimeType(context, file.uri)) {
                "jpg" -> {
                    return "image/jpg"
                }
                "jpeg" -> {
                    return "image/jpeg"
                }
                "png" -> {
                    return "image/png"
                }
                "pdf" -> {
                    return "application/pdf"
                }
                "webp" -> {
                    return "image/webp"
                }
            }
            return "*/*"
        }

        fun deleteFileFromStorage(file: ConvertedFile) {
            val filePath = file.filePath
            if (file.fileType == "pdf" && file.thumbnailUri != null) {
                File(file.thumbnailUri.path!!).delete()
            }
            File(filePath).delete()
        }

        fun startShareSheetSingle(
            activity: FragmentActivity,
            file: File,
            typeString: String
        ): Intent {

            val imageUri = getFileUri(activity, file)

            return Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, imageUri)
                type = typeString
            }
        }


        private fun getDirectoryFilesAsSet(file: File): Set<String> {
            val paths = mutableSetOf<String>()
            val files = file.listFiles()?.map { it.path }

            if (files != null) {
                paths.addAll(files)
            }

            return paths
        }


        fun filterItemsInDirectory(
            file: File,
            items: List<ConvertedFile>
        ): List<ConvertedFile> {
            val dirFiles = getDirectoryFilesAsSet(file)
            return items.filter { it.filePath in dirFiles }
        }


        fun shareSheetMultipleDirectory(
            activity: FragmentActivity,
            files: Array<File>?
        ): Intent {
            val imageUris: ArrayList<Uri> = arrayListOf()
            files?.forEach { file ->
                imageUris.add(getFileUri(activity, file))
            }

            return Intent().apply {
                action = Intent.ACTION_SEND_MULTIPLE
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageUris)
                type = "*/*"
            }
        }

        fun startShareSheetMultiple(
            activity: FragmentActivity,
            files: ArrayList<ConvertedFile>
        ): Intent {
            val imageUris: ArrayList<Uri> = arrayListOf()
            files.forEach { file ->
                val selectedFile = File(file.filePath)
                imageUris.add(getFileUri(activity, selectedFile))
            }

            return Intent().apply {
                action = Intent.ACTION_SEND_MULTIPLE
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageUris)
                type = "*/*"
            }
        }

        fun getFileUri(activity: FragmentActivity, file: File): Uri {
            return FileProvider.getUriForFile(
                activity,
                "com.yuwin.fileconverterpro.fileprovider",
                file
            )
        }

        fun scaleBitmap(input: Bitmap, maxBytes: Long = 1024 * 1024): Bitmap {
            val currentWidth = input.width
            val currentHeight = input.height
            val currentPixels = currentWidth * currentHeight
            val maxPixels = maxBytes / 4
            if (currentPixels <= maxPixels) {
                return input
            }
            val scaleFactor = sqrt(maxPixels / currentPixels.toDouble())
            val newWidthPx = floor(currentWidth * scaleFactor).toInt()
            val newHeightPx = floor(currentHeight * scaleFactor).toInt()
            return Bitmap.createScaledBitmap(input, newWidthPx, newHeightPx, true)
        }

        fun <T> LiveData<T>.observeOnce(lifecycleOwner: LifecycleOwner, observer: Observer<T>) {
            observe(lifecycleOwner, object : Observer<T> {
                override fun onChanged(t: T?) {
                    removeObserver(this)
                    observer.onChanged(t)
                }
            })

        }

        fun getDataString(lastModified: Long): String {
            val df = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
            return df.format(lastModified)
        }

        fun File.copyTo(file: File) {
            inputStream().use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }

        fun getAllFolderColors(context: Context): List<Int> {
            return listOf(
                ContextCompat.getColor(context, R.color.fc1),
                ContextCompat.getColor(context, R.color.fc2),
                ContextCompat.getColor(context, R.color.fc3),
                ContextCompat.getColor(context, R.color.fc4),
                ContextCompat.getColor(context, R.color.fc5),
                ContextCompat.getColor(context, R.color.fc6),
                ContextCompat.getColor(context, R.color.fc7),
                ContextCompat.getColor(context, R.color.fc8),
                ContextCompat.getColor(context, R.color.fc9),
                ContextCompat.getColor(context, R.color.fc10),
                ContextCompat.getColor(context, R.color.fc11),
                ContextCompat.getColor(context, R.color.fc12),
                ContextCompat.getColor(context, R.color.fc13),
                ContextCompat.getColor(context, R.color.fc14),
                ContextCompat.getColor(context, R.color.fc15),
                ContextCompat.getColor(context, R.color.fc16),
                ContextCompat.getColor(context, R.color.fc17),
                ContextCompat.getColor(context, R.color.fc18),
                ContextCompat.getColor(context, R.color.fc19),
                ContextCompat.getColor(context, R.color.fc20),
                ContextCompat.getColor(context, R.color.fc21),
                ContextCompat.getColor(context, R.color.fc22),
                ContextCompat.getColor(context, R.color.fc23),
                ContextCompat.getColor(context, R.color.fc24),
                ContextCompat.getColor(context, R.color.fc25)


            )
        }


    }
}