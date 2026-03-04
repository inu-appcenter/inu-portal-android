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

    fun downloadFile(
        url: String,
        userAgent: String,
        contentDisposition: String?,
        mimeType: String
    ) {
        try {
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setMimeType(mimeType)

                // 쿠키 헤더 설정
                val cookies = CookieManager.getInstance().getCookie(url)
                addRequestHeader("Cookie", cookies)
                addRequestHeader("User-Agent", userAgent)

                // 파일명 추출
                val filename = URLUtil.guessFileName(url, contentDisposition, mimeType)
                val decodedFilename = URLDecoder.decode(filename, "UTF-8")

                setDescription("파일을 다운로드 중입니다.")
                setTitle(decodedFilename)
                setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                )

                // 다운로드 경로를 공용 다운로드 폴더로 설정
                setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    decodedFilename
                )

                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }

            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)

            Toast.makeText(context, "다운로드를 시작했습니다.", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Log.e("Download", "Error: ${e.message}")
            Toast.makeText(
                context,
                "다운로드에 실패했습니다. 관리자에게 문의하세요.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}