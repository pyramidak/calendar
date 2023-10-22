package cz.pyramidak.kalendar.drive

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.webkit.MimeTypeMap
import androidx.appcompat.app.AppCompatActivity
import cz.pyramidak.kalendar.MyApplication
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream


class MyFiles(private val activity: AppCompatActivity) {

    private val app: MyApplication
        get() = (activity.application as MyApplication)

    fun Save(sourceUri: Uri, destination: String, extension: String): Boolean {
        val context = activity.applicationContext
        var bis: BufferedInputStream? = null
        var bos: BufferedOutputStream? = null
        var input: InputStream? = null
        var hasError = false
        try {

            if (isVirtualFile(context, sourceUri)) {
                input = getInputStreamForVirtualFile(context, sourceUri, getMimeType(extension) ?:"")
            } else {
                input = context.contentResolver.openInputStream(sourceUri)
            }
            val originalsize = input!!.available()
            bis = BufferedInputStream(input)
            bos = BufferedOutputStream(FileOutputStream(destination))
            val buf = ByteArray(originalsize)
            bis.read(buf)
            do {
                bos.write(buf)
            } while (bis.read(buf) != -1)

        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            hasError = true
        } finally {
            try {
                if (bos != null) {
                    bos.flush()
                    bos.close()
                }
            } catch (ignored: java.lang.Exception) {
            }
        }
        return !hasError
    }

    private fun replaceFileWithDir(path: String): Boolean {
        val file = File(path)
        if (!file.exists()) {
            if (file.mkdirs()) {
                return true
            }
        } else if (file.delete()) {
            val folder = File(path)
            if (folder.mkdirs()) {
                return true
            }
        }
        return false
    }

    private fun isVirtualFile(context: Context, uri: Uri): Boolean {
        if (!DocumentsContract.isDocumentUri(context, uri)) { return false }
        val cursor = context.contentResolver.query(uri, arrayOf(DocumentsContract.Document.COLUMN_FLAGS), null, null, null)
        var flags = 0
        if (cursor!!.moveToFirst()) { flags = cursor.getInt(0) }
        cursor.close()
        return flags and DocumentsContract.Document.FLAG_VIRTUAL_DOCUMENT != 0
    }

    @Throws(IOException::class)
    private fun getInputStreamForVirtualFile(context: Context, uri: Uri, mimeTypeFilter: String): InputStream? {
        val resolver = context.contentResolver
        val openableMimeTypes = resolver.getStreamTypes(uri, mimeTypeFilter)
        if (openableMimeTypes.isNullOrEmpty()) {
            throw FileNotFoundException()
        }
        return resolver.openTypedAssetFileDescriptor(uri, openableMimeTypes[0], null)?.createInputStream()
    }

    private fun getMimeType(url: String): String? {
        var type: String? = null
        val extension = MimeTypeMap.getFileExtensionFromUrl(url)
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        }
        return type
    }
}