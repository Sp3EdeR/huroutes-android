package hu.speeder.huroutes.web.downloaders

import android.content.Context
import android.net.Uri

/**
 * Exception class thrown when the URI cannot be handled.
 */
class UnknownUriType(uri: Uri): Exception("Cannot download uri type: $uri")

/**
 * Returns a `Downloader` to handle the given URI.
 */
fun getDownloaderFor(context: Context, uri: Uri): Downloader {
    val downloader = when(uri.scheme) {
        "data" -> DataDownloader(uri)
        else -> throw UnknownUriType(uri)
    }
    return downloader.setContext(context)
}
