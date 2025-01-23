package inu.appcenter.intip_android.ui.component

import android.util.Log
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun CustomAndroidView(
    modifier: Modifier = Modifier,
    path: String,
    token: String? = null  // 추가: 토큰
) {
    val context = LocalContext.current  // Composable 함수 외부에서 context를 얻음
    val WEB_BASE_URL = "https://intip.inuappcenter.kr/app"
    val url = "$WEB_BASE_URL$path"

    val webView = remember { WebView(context) }

    // 토큰이 있는 경우, extra headers를 구성합니다.
//    val headers = if (!token.isNullOrEmpty()) {
//        mapOf("Auth" to "$token")
//    } else {
//        emptyMap()
//    }

    // Initialize the WebView only once
    DisposableEffect(webView) {
        webView.apply {
            // 레이아웃 파라미터 설정
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            // 디버깅 활성화
            WebView.setWebContentsDebuggingEnabled(true)

            // WebView 설정
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.setSupportZoom(false)
            settings.displayZoomControls = false

            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                    // 원하는 대로 처리 (필터링, 로깅 등)
                    Log.d(
                        "WebViewConsole",
                        "[${consoleMessage.messageLevel()}] " +
                                "${consoleMessage.message()} " +
                                "(line: ${consoleMessage.lineNumber()}, source: ${consoleMessage.sourceId()})"
                    )
                    return super.onConsoleMessage(consoleMessage)
                }
            }

            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    request?.let {
                        Log.d("WebViewRequest", "URL: ${it.url}")
                        it.requestHeaders.forEach { header ->
                            Log.d("WebViewRequest", "${header.key}: ${header.value}")
                        }
                    }
                    return super.shouldInterceptRequest(view, request)
                }
            }

            // 초기 로딩은 하지 않습니다.
        }

        onDispose {
            webView.destroy()
        }
    }

    AndroidView(
        modifier = modifier
            .fillMaxSize()
            .padding(),
        factory = { webView },
        update = { view ->
            if (!token.isNullOrEmpty()) {
                val headers = mapOf("Auth" to token)
                Log.d("WebViewUpdate", "Updating URL with headers: $headers")
                view.loadUrl(url, headers)
            } else {
                Log.d("WebViewUpdate", "Updating URL without headers")
                view.loadUrl(url)
            }
        }
    )
}