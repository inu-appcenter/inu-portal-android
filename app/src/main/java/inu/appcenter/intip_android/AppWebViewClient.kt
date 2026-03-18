package inu.appcenter.intip_android

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import java.net.URISyntaxException

class AppWebViewClient(
    private val context: Context,
    private val onPageStartedCallback: ((String?) -> Unit)? = null,
    private val onPageFinishedCallback: (String?) -> Unit
) : WebViewClient() {

    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
        super.onPageStarted(view, url, favicon)
        onPageStartedCallback?.invoke(url)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        view?.let { FcmTokenBridge.refreshCurrentToken(context, it) }
        onPageFinishedCallback(url)
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)
        onPageFinishedCallback(request?.url.toString())
    }

    override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
        super.doUpdateVisitedHistory(view, url, isReload)
        onPageFinishedCallback(url)
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url.toString()

        if (Constants.ALLOWED_DOMAINS.any { url.startsWith(it) }) {
            onPageStartedCallback?.invoke(url)
            return false
        }

        return handleExternalScheme(url, view)
    }

    private fun handleExternalScheme(url: String, view: WebView?): Boolean {
        if (url.startsWith("intent:") || url.startsWith("market:") || url.startsWith("ispmobile:")) {
            return try {
                val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    true
                } else {
                    handleAppNotInstalled(intent, view)
                }
            } catch (error: URISyntaxException) {
                Log.e("WebView", "Intent Parse Error: ${error.message}")
                false
            } catch (error: Exception) {
                Log.e("WebView", "External App Error: ${error.message}")
                true
            }
        }

        return try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            true
        } catch (error: Exception) {
            Toast.makeText(context, "외부 링크 열기 실패", Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun handleAppNotInstalled(intent: Intent, view: WebView?): Boolean {
        val fallbackUrl = intent.getStringExtra("browser_fallback_url")
        if (fallbackUrl != null) {
            view?.loadUrl(fallbackUrl)
            return true
        }

        val packageName = intent.`package`
        if (packageName != null) {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
            return true
        }

        Toast.makeText(context, "관련 앱이 설치되어 있지 않습니다.", Toast.LENGTH_SHORT).show()
        return true
    }
}
