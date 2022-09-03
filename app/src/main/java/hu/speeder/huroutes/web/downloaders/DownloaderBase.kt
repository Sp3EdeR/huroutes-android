package hu.speeder.huroutes.web.downloaders

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import java.io.OutputStream

class CannotWriteFileException(uri: String): Exception("Unable to write file $uri")

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
        val uri = resolver.insert(MediaStore.Files.getContentUri("external"), values)
        uri?.also {
            resolver.openOutputStream(it,"wt")?.also { stream ->
                return Pair(it, stream)
            }
        }
        throw CannotWriteFileException("$directory/$fileName")
    }
}
