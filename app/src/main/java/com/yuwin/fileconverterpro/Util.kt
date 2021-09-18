package com.yuwin.fileconverterpro

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.os.SystemClock
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.TypedValue
import android.view.View
import android.webkit.MimeTypeMap
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.yuwin.fileconverterpro.db.ConvertedFile
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.lang.Exception
import java.lang.String.format
import java.text.CharacterIterator
import java.text.SimpleDateFormat
import java.text.StringCharacterIterator
import java.util.*
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

        fun deleteFileFromStorage(file: ConvertedFile, context: Context?) {
            val filePath = file.filePath
            if (file.fileType == "pdf" && file.thumbnailUri != null) {
                File(file.thumbnailUri.path!!).delete()
            }
            file.publicUri?.let { deleteFileFromPublicStorage(context, it) }
            File(filePath).delete()
        }

        fun deleteFileFromPublicStorage(context: Context?, publicUri: Uri) {
            try {
                context?.contentResolver?.delete(publicUri, null, null)
            }catch (e: Exception) {

            }
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

        fun getFilename(app: Context, uri: Uri): String? {
            val cursor = app.contentResolver?.query(uri, null, null, null, null)
            var filename: String? = null

            cursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)?.let { nameIndex ->
                cursor.moveToFirst()

                filename = cursor.getString(nameIndex)
                cursor.close()
            }

            return filename
        }

        @Throws(IOException::class)
        fun saveBitmap(
            context: Context, bitmap: Bitmap, format: Bitmap.CompressFormat,
            mimeType: String, displayName: String,
            path: String = Environment.DIRECTORY_PICTURES + File.separator + "Image Converter"
        ): Uri {

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, path)
            }

            val resolver = context.contentResolver
            var uri: Uri? = null

            try {
                uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: throw IOException("Failed to create new MediaStore record.")

                resolver.openOutputStream(uri)?.use {
                    if (!bitmap.compress(format, 95, it))
                        throw IOException("Failed to save bitmap.")
                } ?: throw IOException("Failed to open output stream.")

                return uri

            } catch (e: IOException) {

                uri?.let { orphanUri ->
                    // Don't leave an orphan entry in the MediaStore
                    resolver.delete(orphanUri, null, null)
                }

                throw e
            }
        }

        @Throws(IOException::class)
        fun savePDFFile(
            context: Context, document: PdfDocument, displayName: String
        ): Uri {


            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DOCUMENTS + File.separator + "Image Converter"
                )
            }

            val resolver = context.contentResolver
            var uri: Uri? = null

            try {
                uri = resolver.insert(MediaStore.Files.getContentUri("external"), values)
                    ?: throw IOException("Failed to create new MediaStore record.")

                val outputStream: OutputStream? = context.contentResolver.openOutputStream(uri)

                document.writeTo(outputStream)

                outputStream?.close()

                return uri

            } catch (e: IOException) {

                uri?.let { orphanUri ->
                    // Don't leave an orphan entry in the MediaStore
                    resolver.delete(orphanUri, null, null)
                }

                throw e
            }
        }

        @Throws(IOException::class)
        fun saveFolder(context: Context, path: String): Uri {
            val contentValues = ContentValues()
            contentValues.put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                path
            )

            val resolver = context.contentResolver
            var uri: Uri? = null

            try {
                uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
                    ?: throw IOException("Failed to create new MediaStore record.")

                val folder = File(uri.toString())
                val isCreated = folder.exists()
                if (!isCreated) {
                    folder.mkdirs()
                }
                return uri
            } catch (e: IOException) {

                uri?.let { orphanUri ->
                    // Don't leave an orphan entry in the MediaStore
                    resolver.delete(orphanUri, null, null)
                }

                throw e
            }

        }

        fun dipToPixels(context: Context, dipValue: Float): Float {
            val metrics = context.resources.displayMetrics
            return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, metrics)
        }

        class OnSingleClickListener(private val block: () -> Unit) : View.OnClickListener {

            private var lastClickTime = 0L

            override fun onClick(view: View) {
                if (SystemClock.elapsedRealtime() - lastClickTime < 1000) {
                    return
                }
                lastClickTime = SystemClock.elapsedRealtime()

                block()
            }
        }

        fun View.setOnSingleClickListener(block: () -> Unit) {
            setOnClickListener(OnSingleClickListener(block))
        }


    }
}