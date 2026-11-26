package app.picasso.tracer.ui.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import app.picasso.tracer.R
import java.io.File

/** Opens the system share sheet for a stored reference image. False if it can't be shared. */
fun shareImage(context: Context, file: File): Boolean {
    if (!file.exists()) return false
    val uri = runCatching {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }.getOrNull() ?: return false
    return startShare(context, uri, "image/jpeg")
}

/** Opens the system share sheet for a saved recording (a MediaStore content uri). */
fun shareVideo(context: Context, uri: Uri): Boolean = startShare(context, uri, "video/mp4")

private fun startShare(context: Context, uri: Uri, mimeType: String): Boolean {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(send, context.getString(R.string.share_chooser_title)).apply {
        // Applications may start this from a non-activity context.
        if (context !is android.app.Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return try {
        context.startActivity(chooser)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
