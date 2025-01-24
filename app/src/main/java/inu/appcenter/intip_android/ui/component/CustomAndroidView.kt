package inu.appcenter.intip_android.ui.component

import android.content.Intent
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
                mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
            }

            // JavaScript Interface 추가
            addJavascriptInterface(AndroidBridge(navController), "AndroidBridge")

//            webChromeClient = object : WebChromeClient() {
//                override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
//                    Log.d(
//                        "WebViewConsole",
//                        "[${consoleMessage.messageLevel()}] " +
//                                "${consoleMessage.message()} " +
//                                "(line: ${consoleMessage.lineNumber()}, source: ${consoleMessage.sourceId()})"
//                    )
//                    return super.onConsoleMessage(consoleMessage)
//                }
//            }

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    request?.url?.let { uri ->
                        if (uri.host != "intip.inuappcenter.kr") {
                            // 허용되지 않은 도메인 접근 시 네이티브 브라우저로 열기
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            context.startActivity(intent)
                            return true
                        }

                        when (uri.path) {
                            Routes.MYPAGE -> {
                                navigateToDestination(navController, "mypage", AllDestination.MyPage.route)
                                return true
                            }
                            Routes.SAVE -> {
                                navigateToDestination(navController, "save", AllDestination.Save.route)
                                return true
                            }
                            Routes.WRITE -> {
                                navigateToDestination(navController, "write", AllDestination.Write.route)
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
    fun navigateTo(tab: String, route: String) {
        Handler(Looper.getMainLooper()).post {
            navigateToDestination(navController, tab, route)
        }
    }
}

/**
 * 네이티브 네비게이션을 처리하는 공통 함수
 */
private fun navigateToDestination(navController: NavController, tab: String, route: String) {
    when (tab) {
        "mypage" -> {
            if (navController.currentDestination?.route != AllDestination.MyPage.route) {
                navController.navigate(AllDestination.MyPage.route) {
                    popUpTo(AllDestination.Main.route) { inclusive = false }
                }
            }
        }
        "save" -> {
            if (navController.currentDestination?.route != AllDestination.Save.route) {
                navController.navigate(AllDestination.Save.route) {
                    popUpTo(AllDestination.Main.route) { inclusive = false }
                }
            }
        }
        "write" -> {
            if (navController.currentDestination?.route != AllDestination.Write.route) {
                navController.navigate(AllDestination.Write.route) {
                    popUpTo(AllDestination.Main.route) { inclusive = false }
                }
            }
        }
        // 필요한 다른 탭 추가
        else -> {
            Log.w("AndroidBridge", "Unknown tab: $tab")
        }
    }

    // 추가로 route에 따라 페이지를 로드하는 로직을 추가할 수 있습니다.
    // 예: 특정 탭 내에서 특정 페이지로 이동
}
