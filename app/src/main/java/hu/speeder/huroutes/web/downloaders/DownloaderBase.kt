package hu.speeder.huroutes.web.downloaders

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.io.OutputStream

/**
 * An exception which is thrown when a file cannot be written.
 */
class CannotWriteFileException(path: String, cause: Throwable? = null): Exception("Unable to write file $path", cause)

/**
 * A downloader for the specified uri, returned by the `getDownloaderFor` factory.
 */
interface Downloader {
    /**
     * Returns the Android permissions needed for the downloader operation.
     */
    val permissionsNeeded: Array<String>

    /**
     * Gives the downloader a context.
     */
    fun setContext(context: Context): Downloader

    /**
     * Saves the file to the specified directory.
     * @return A pair of the uri and file names.
     */
    fun saveTo(directory: String): Pair<Uri, String>
}

/**
 * A base class that helps the implementation of `Downloader`s.
 */
abstract class DownloaderBase: Downloader {
    private var _context: Context? = null
    protected val context get() = _context!!

    override val permissionsNeeded: Array<String> = arrayOf()

    override fun setContext(context: Context): Downloader {
        _context = context
        return this
    }

    /**
     * A method to create a file stream to write the downloaded data to.
     * @param directory The relative directory to which the file will be added.
     * @param fileName The name of the file that's being created.
     * @param mimeType The MIME type of the file being created.
     * @return A pair of the `content://` URI and output stream of the new file.
     */
    protected fun createMediaFileStream(
        directory: String, fileName: String,
        mimeType: String?
    ): Pair<Uri, OutputStream> {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.RELATIVE_PATH, directory)
            if (mimeType != null) {
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            }
        }
        val resolver = context.applicationContext.contentResolver
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Files.getContentUri("external")
        }
        val uri = resolver.insert(collection, values)
            ?: throw CannotWriteFileException("$directory/$fileName")

        try {
            val stream = resolver.openOutputStream(uri, "wt")
                ?: throw CannotWriteFileException("$directory/$fileName")
            return Pair(uri, stream)
        } catch (exception: Exception) {
            resolver.delete(uri, null, null)
            when (exception) {
                is CannotWriteFileException -> throw exception
                else -> throw CannotWriteFileException("$directory/$fileName", exception)
            }
        }
    }
}
