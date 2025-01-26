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
    // WebView 인스턴스 생성 (remember로 구성 생명주기 내에서 유지)
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
                    // 1) 로그로 확인
                    Log.d("CustomWebView", "shouldOverrideUrlLoading 호출됨, request = $request")

                    request?.url?.let { uri ->
                        Log.d("CustomWebView", "Intercepted URL: $uri")

                        // 1) 외부 도메인 체크
                        if (uri.host != "intip.inuappcenter.kr") {
                            Log.d("CustomWebView", "외부 도메인 -> 브라우저 열기: ${uri.host}")
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            context.startActivity(intent)
                            return true
                        }

                        // 2) path / query 파라미터 확인
                        val rawPath = uri.path ?: ""
                        // "/app/postdetail"라면 -> "/postdetail"
                        val cleanedPath = if (rawPath.startsWith("/app")) {
                            rawPath.removePrefix("/app")
                        } else {
                            rawPath
                        }

                        val queryId = uri.getQueryParameter("id")

                        Log.d("CustomWebView", "HOST: ${uri.host}, PATH: $path, QUERY_ID: $queryId")

                        // ---------------------
                        // [CASE A] 동적 라우트에 해당하는지 확인 (ex: /postdetail)
                        // ---------------------
                        if (dynamicRoutesMap.containsKey(cleanedPath)) {
                            val destination = dynamicRoutesMap[cleanedPath]!!
                            if (queryId.isNullOrEmpty()) {
                                Log.w("CustomWebView", "id가 없어서 처리 불가 -> 웹뷰로 처리")
                                return super.shouldOverrideUrlLoading(view, request)
                            }

                            // id가 있으니 네이티브 라우트로 이동
                            when (destination) {
                                is AllDestination.PostDetail -> {
                                    navController.navigate(destination.createRoute(queryId))
                                    Log.d("CustomWebView", "네비게이트 -> ${destination.createRoute(queryId)}")
                                }
                                is AllDestination.CouncilNoticeDetail -> {
                                    navController.navigate(destination.createRoute(queryId))
                                    Log.d("CustomWebView", "네비게이트 -> ${destination.createRoute(queryId)}")
                                }
                                is AllDestination.PetitionDetail -> {
                                    navController.navigate(destination.createRoute(queryId))
                                    Log.d("CustomWebView", "네비게이트 -> ${destination.createRoute(queryId)}")
                                }
                                else -> {
                                    Log.w("CustomWebView", "알 수 없는 동적 라우트 대상: $destination")
                                }
                            }
                            return true
                        }

                        // ---------------------
                        // [CASE B] 정적 라우트 path인지 확인
                        // ---------------------
                        // 2) 정적 라우트
                        val staticDestination = AllDestination.webViewPage.find { it.webPath == cleanedPath }
                        if (staticDestination != null) {
                            navController.navigate(staticDestination.route)
                            return true
                        }

                        // ---------------------
                        // [CASE C] 매핑되지 않은 path
                        // ---------------------
                        Log.d("CustomWebView", "매핑되지 않은 path -> 웹뷰에서 처리: $path")
                        return super.shouldOverrideUrlLoading(view, request)
                    }
                    return super.shouldOverrideUrlLoading(view, request)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    Log.d("CustomWebView", "onPageFinished: $url")
                    // 토큰이 있으면 웹 localStorage에 주입 (JS로 localStorage.setItem)
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
                    request?.url?.let {
                        Log.d("WebViewRequest", "shouldInterceptRequest: $it")
                    }
                    return super.shouldInterceptRequest(view, request)
                }
            }
        }

        // onDispose 시 WebView 자원 정리
        onDispose {
            Log.d("CustomWebView", "onDispose 호출 -> webView.destroy()")
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
    Log.d("AndroidBridge", "navigateToDestination: tab=$tab, route=$route")
    when (tab) {
        "mypage" -> {
            if (navController.currentDestination?.route != AllDestination.MyPage.route) {
                navController.navigate(AllDestination.MyPage.route) {
                    popUpTo(AllDestination.Home.route) { inclusive = false }
                }
            }
        }
        "save" -> {
            if (navController.currentDestination?.route != AllDestination.Save.route) {
                navController.navigate(AllDestination.Save.route) {
                    popUpTo(AllDestination.Home.route) { inclusive = false }
                }
            }
        }
        "write" -> {
            if (navController.currentDestination?.route != AllDestination.Write.route) {
                navController.navigate(AllDestination.Write.route) {
                    popUpTo(AllDestination.Home.route) { inclusive = false }
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

private val dynamicRoutesMap: Map<String, AllDestination> = mapOf(
    "/postdetail" to AllDestination.PostDetail,
    "/councilnoticedetail" to AllDestination.CouncilNoticeDetail,
    "/petitiondetail" to AllDestination.PetitionDetail
    // 필요 시 더 추가
)

