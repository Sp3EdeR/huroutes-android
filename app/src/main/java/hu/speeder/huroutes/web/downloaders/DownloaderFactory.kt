package hu.speeder.huroutes.web.downloaders

import android.net.Uri

/**
 * Exception class thrown when the URI cannot be handled.
 */
class UnknownUriType(uri: Uri): Exception("Cannot download uri type: $uri")

/**
 * Returns a `Downloader` to handle the given URI.
 */
fun getDownloaderFor(uri: Uri): Downloader {
    return when(uri.scheme) {
        "data" -> DataDownloader(uri)
        else -> throw UnknownUriType(uri)
    }
}