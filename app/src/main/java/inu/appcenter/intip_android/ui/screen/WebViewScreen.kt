package inu.appcenter.intip_android.ui.screen

import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import inu.appcenter.intip_android.ui.component.AppBottomBar

@Composable
fun WebViewScreen(modifier: Modifier = Modifier, navController: NavHostController, path: String) {
    //todo: 나중에 전역 변수나 env로 빼기
    val WEB_BASE_URL = "https://intip.inuappcenter.kr/app"

    Scaffold(
        bottomBar = {
            AppBottomBar(navController)
        }
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .padding(it),
            factory = { context ->
                WebView(context).apply {
                    // 디버깅 활성화
                    WebView.setWebContentsDebuggingEnabled(true)

                    // WebView 설정
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    settings.setSupportZoom(true)
                    //settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    settings.mixedContentMode = MIXED_CONTENT_ALWAYS_ALLOW
                    settings.cacheMode = WebSettings.LOAD_DEFAULT

                    webViewClient = WebViewClient()

                    webChromeClient = object : WebChromeClient() {
                        /**
                         * 콘솔 메시지를 캡처하기 위해 오버라이드
                         */
                        override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                            // 원하는 대로 처리(필터링, 로깅 등)
                            Log.d(
                                "WebViewConsole",
                                "[${consoleMessage.messageLevel()}] " +
                                        "${consoleMessage.message()} " +
                                        "(line: ${consoleMessage.lineNumber()}, source: ${consoleMessage.sourceId()})"
                            )
                            return super.onConsoleMessage(consoleMessage)
                        }
                    }
                    loadUrl("${WEB_BASE_URL}${path}")
                }
            },
            update = { webView ->
                webView.loadUrl("${WEB_BASE_URL}${path}")
            }
        )
    }
}
