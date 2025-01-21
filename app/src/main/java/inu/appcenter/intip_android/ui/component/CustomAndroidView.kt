package inu.appcenter.intip_android.ui.component

import android.util.Log
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun CustomAndroidView(
    modifier: Modifier = Modifier,
    path: String,
    token: String? = null  // 추가: 토큰
) {
    val WEB_BASE_URL = "https://intip.inuappcenter.kr/app"
    val url = "$WEB_BASE_URL$path"
    // 토큰이 있는 경우, extra headers를 구성합니다.
    val headers = if (!token.isNullOrEmpty()) {
        mapOf("Auth" to "$token")
    } else {
        emptyMap()
    }

    AndroidView(
        modifier = modifier
            .fillMaxSize()
            .padding(),
        factory = { context ->
            WebView(context).apply {
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
                // extra headers를 포함하여 URL 로드
                loadUrl(url, headers)
            }
        },
        update = { webView ->
            // 업데이트 시에도 extra headers와 함께 로드
            webView.loadUrl(url, headers)
        }
    )
}