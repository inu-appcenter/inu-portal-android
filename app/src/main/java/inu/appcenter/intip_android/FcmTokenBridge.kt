package inu.appcenter.intip_android

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.WebView
import com.google.firebase.messaging.FirebaseMessaging
import org.json.JSONObject
import java.lang.ref.WeakReference

object FcmTokenBridge {

    private const val PREFS_NAME = "intip_fcm"
    private const val KEY_TOKEN = "latest_token"

    private val mainHandler = Handler(Looper.getMainLooper())
    private var webViewRef: WeakReference<WebView>? = null

    fun attachWebView(webView: WebView) {
        webViewRef = WeakReference(webView)
    }

    fun detachWebView(webView: WebView? = null) {
        val attachedWebView = webViewRef?.get()
        if (webView == null || attachedWebView === webView) {
            webViewRef = null
        }
    }

    fun refreshCurrentToken(context: Context, webView: WebView? = webViewRef?.get()) {
        val appContext = context.applicationContext

        getStoredToken(appContext)?.let { token ->
            webView?.let { dispatchTokenToWebView(it, token) }
        }

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM_TOKEN", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result?.trim().orEmpty()
            if (token.isEmpty()) {
                return@addOnCompleteListener
            }

            updateToken(appContext, token, webView)
        }
    }

    fun updateToken(context: Context, token: String, webView: WebView? = webViewRef?.get()) {
        val normalizedToken = token.trim()
        if (normalizedToken.isEmpty()) {
            return
        }

        saveToken(context.applicationContext, normalizedToken)
        webView?.let { dispatchTokenToWebView(it, normalizedToken) }
    }

    private fun dispatchTokenToWebView(webView: WebView, token: String) {
        val normalizedToken = token.trim()
        if (normalizedToken.isEmpty()) {
            return
        }

        mainHandler.post {
            if (webView.url.isNullOrBlank()) {
                return@post
            }

            val tokenLiteral = JSONObject.quote(normalizedToken)
            val script =
                "window.onReceiveFcmToken && window.onReceiveFcmToken($tokenLiteral);"

            webView.evaluateJavascript(script, null)
        }
    }

    private fun saveToken(context: Context, token: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TOKEN, token)
            .apply()
    }

    private fun getStoredToken(context: Context): String? {
        val token = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_TOKEN, null)
            ?.trim()

        return token?.takeIf { it.isNotEmpty() }
    }
}
