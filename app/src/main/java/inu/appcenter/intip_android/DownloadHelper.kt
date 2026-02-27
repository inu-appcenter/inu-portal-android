package inu.appcenter.intip_android

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.widget.Toast
import java.net.URLDecoder

class DownloadHelper(private val context: Context) {

    fun downloadFile(url: String, userAgent: String, contentDisposition: String?, mimeType: String) {
        try {
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setMimeType(mimeType)

                // ì¿ í‚¤ ë°??¤ë” ?¤ì •
                val cookies = CookieManager.getInstance().getCookie(url)
                addRequestHeader("Cookie", cookies)
                addRequestHeader("User-Agent", userAgent)

                // ?Œì¼ëª?ì¶”ì¶œ
                val filename = URLUtil.guessFileName(url, contentDisposition, mimeType)
                val decodedFilename = URLDecoder.decode(filename, "UTF-8")

                setDescription("?Œì¼ ?¤ìš´ë¡œë“œ ì¤?..")
                setTitle(decodedFilename)
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, decodedFilename)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }

            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
            Toast.makeText(context, "?¤ìš´ë¡œë“œë¥??œì‘?©ë‹ˆ??", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Log.e("Download", "Error: ${e.message}")
            Toast.makeText(context, "?¤ìš´ë¡œë“œ ?¤íŒ¨: ê´€ë¦¬ì?ê²Œ ë¬¸ì˜?˜ì„¸??", Toast.LENGTH_SHORT).show()
        }
    }
}
