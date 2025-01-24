package inu.appcenter.intip_android.ui.component

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import inu.appcenter.intip_android.ui.navigate.AllDestination
import inu.appcenter.intip_android.ui.navigate.Routes
import java.io.UnsupportedEncodingException

@Composable
fun CustomAndroidView(
    modifier: Modifier = Modifier,
    path: String,
    token: String? = null,
    navController: NavController
) {
    val context = LocalContext.current
    val WEB_BASE_URL = "https://intip.inuappcenter.kr/app"

    val url = try {
        token?.let {
            val baseUri = Uri.parse("$WEB_BASE_URL$path")
            val urlBuilder = baseUri.buildUpon()
            urlBuilder.appendQueryParameter("token", it)
            urlBuilder.toString()
        } ?: "$WEB_BASE_URL$path"
    } catch (e: UnsupportedEncodingException) {
        Log.e("CustomAndroidView", "URL 인코딩 에러: ${e.message}")
        "$WEB_BASE_URL$path"
    }

    val webView = remember { WebView(context) }

    DisposableEffect(webView) {
        webView.apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            WebView.setWebContentsDebuggingEnabled(true)

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                useWideViewPort = true
                loadWithOverviewMode = true
                setSupportZoom(false)
                displayZoomControls = false
            }

            // JavaScript Interface 추가
            addJavascriptInterface(AndroidBridge(navController), "AndroidBridge")

            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
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
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    request?.url?.let { uri ->
                        when (uri.path) {
                            Routes.MYPAGE -> {
                                navigateToDestination(navController, AllDestination.MyPage.route)
                                return true // WebView에서 로드하지 않도록 함
                            }
                            Routes.SAVE -> {
                                navigateToDestination(navController, AllDestination.Save.route)
                                return true
                            }
                            // 추가적인 경로가 필요하면 여기에 추가
                            else -> {
                                // 다른 경로는 WebView가 처리하도록 함
                                return super.shouldOverrideUrlLoading(view, request)
                            }
                        }
                    }
                    return super.shouldOverrideUrlLoading(view, request)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    token?.let {
                        view?.evaluateJavascript(
                            "localStorage.setItem('accessToken', '$it');",
                            null
                        )
                    }
                }

                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    request?.let {
                        Log.d("WebViewRequest", "URL: ${it.url}")
                    }
                    return super.shouldInterceptRequest(view, request)
                }
            }
        }

        onDispose {
            webView.destroy()
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize().padding(),
        factory = { webView },
        update = { view ->
            Log.d("WebViewUpdate", "Loading URL: $url")
            view.loadUrl(url)
        }
    )
}

/**
 * JavaScript와 네이티브 간의 브리지를 담당하는 클래스
 */
class AndroidBridge(private val navController: NavController) {
    @JavascriptInterface
    fun navigateTo(route: String) {
        Handler(Looper.getMainLooper()).post {
            navigateToDestination(navController, route)
        }
    }
}

/**
 * 네이티브 네비게이션을 처리하는 공통 함수
 */
private fun navigateToDestination(navController: NavController, route: String) {
    if (navController.currentDestination?.route != route) {
        navController.navigate(route) {
            // 필요한 네비게이션 옵션 설정
            popUpTo(AllDestination.Main.route) { inclusive = false }
        }
    }
}