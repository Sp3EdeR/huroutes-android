package hu.speeder.huroutes.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import hu.speeder.huroutes.R

/**
 * An abstract base class for notification generation.
 * Subclass this to create notification types.
 */
abstract class Notification(protected val context: Context) {
    companion object {
        private var nextUniqueId = 1
        val uniqueId get() = ++nextUniqueId
    }

    protected abstract val channelId: String
    protected abstract val channelName: String
    protected abstract val channelDescription: String
    protected abstract val priority: Int
    protected abstract val vibrationPattern: LongArray
    protected abstract val iconId: Int
    protected abstract val title: String
    protected abstract val textTemplate: Int

    private var _id = -1
    @Suppress("unused")
    val id: Int get() {
        if (_id == -1)
            _id = uniqueId
        return _id
    }
    @Suppress("unused")
    fun setId(id: Int): Notification {
        _id = id
        return this
    }
    @Suppress("unused")
    fun uniqueId(): Notification {
        _id = uniqueId
        return this
    }

    /**
     * Shows this notification
     * @param args Format string arguments for the text template.
     */
    fun showText(vararg args: Any?) {
        val text = context.getString(textTemplate, *args)

        // Create the notification
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(iconId)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(priority)
            .setVibrate(vibrationPattern)

        onCustomizeNotification(builder)

        // Show the notification
        NotificationManagerCompat.from(context).notify(id, builder.build())
    }

    /**
     * Initialization function to be called from subclasses after initializing the properties.
     */
    protected fun init() {
        createChannel()
    }

    /**
     * Override this function to customize the notification before it is shown.
     */
    protected open fun onCustomizeNotification(builder: NotificationCompat.Builder) { }

    /**
     * Initializes the notification's channel for Android O and up.
     */
    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val channel = NotificationChannel(channelId, channelName, getNotificationPriority())
        channel.description = channelDescription
        channel.vibrationPattern = vibrationPattern
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?)?.apply {
            createNotificationChannel(channel)
        }
    }

    /**
     * Creates a mapping between the old and new notification priority/importance values.
     * This allows the interface to only specify the old-style value.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun getNotificationPriority(): Int {
        return when (priority) {
            NotificationCompat.PRIORITY_MIN -> NotificationManager.IMPORTANCE_MIN
            NotificationCompat.PRIORITY_LOW -> NotificationManager.IMPORTANCE_LOW
            NotificationCompat.PRIORITY_DEFAULT -> NotificationManager.IMPORTANCE_DEFAULT
            NotificationCompat.PRIORITY_HIGH -> NotificationManager.IMPORTANCE_HIGH
            NotificationCompat.PRIORITY_MAX -> NotificationManager.IMPORTANCE_MAX
            else -> NotificationManager.IMPORTANCE_DEFAULT
        }
    }
}

/**
 * Shows a notification for a file download's completion.
 */
class DownloadNotification(context: Context): Notification(context) {
    override val channelId = "DOWNLOAD_NOTIFICATION"
    override val channelName = context.getString(R.string.dlnoti_channel)
    override val channelDescription = context.getString(R.string.dlnoti_desc)
    override val priority = NotificationCompat.PRIORITY_HIGH
    override val vibrationPattern = LongArray(0)
    override val iconId = R.drawable.ic_file_download_done
    override val title = context.getString(R.string.dlnoti_title)
    override val textTemplate = R.string.dlnoti_text

    init { init() }

    private var intentUri: Uri? = null
    fun setIntentUri(uri: Uri): DownloadNotification {
        intentUri = uri
        return this
    }

    /**
     * Customizes the notification with a click intent to open the downloaded content file.
     */
    override fun onCustomizeNotification(builder: NotificationCompat.Builder) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, intentUri).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val pendingIntent: PendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                if (Build.VERSION_CODES.M <= Build.VERSION.SDK_INT) PendingIntent.FLAG_IMMUTABLE else 0
            )
            builder.setContentIntent(pendingIntent)
            builder.setAutoCancel(true)
        }
        catch (e: Exception) {
        }
    }
}