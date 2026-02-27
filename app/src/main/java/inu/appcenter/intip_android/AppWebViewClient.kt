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
import com.google.firebase.messaging.FirebaseMessaging
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

        // FCM ?†ÌÅ∞ Ï£ºÏûÖ
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                view?.evaluateJavascript("window.onReceiveFcmToken && window.onReceiveFcmToken('${task.result}');", null)
            }
        }

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

        // 1. ?¥Î? ?ÑÎ©î???àÏö©
        if (Constants.ALLOWED_DOMAINS.any { url.startsWith(it) }) {
            onPageStartedCallback?.invoke(url)
            return false
        }

        // 2. ?∏Î? ???§ÌÇ§Îß?Ï≤òÎ¶¨
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
            } catch (e: URISyntaxException) {
                Log.e("WebView", "Intent Parse Error: ${e.message}")
                false
            } catch (e: Exception) {
                Log.e("WebView", "External App Error: ${e.message}")
                true
            }
        }

        // 3. ?ºÎ∞ò ?∏Î? ÎßÅÌÅ¨
        return try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            true
        } catch (e: Exception) {
            Toast.makeText(context, "?∏Î? ÎßÅÌÅ¨ ?¥Í∏∞ ?§Ìå®", Toast.LENGTH_SHORT).show()
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

        Toast.makeText(context, "Í¥Ä???±Ïù¥ ?§Ïπò?òÏñ¥ ?àÏ? ?äÏäµ?àÎã§.", Toast.LENGTH_SHORT).show()
        return true
    }
}
