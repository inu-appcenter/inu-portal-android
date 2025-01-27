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

    // URL과 쿼리 파라미터 처리
    val url = try {
        val baseUri = Uri.parse("$WEB_BASE_URL$path")
        val urlBuilder = baseUri.buildUpon()

        // 토큰이 있다면 추가
        token?.let {
            urlBuilder.appendQueryParameter("token", it)
        }

        // council 페이지의 경우 기본 type=notice 추가
        if (path == "/home/council" && !baseUri.getQueryParameter("type").isNullOrEmpty()) {
            urlBuilder.appendQueryParameter("type", "notice")
        }
        // util 페이지의 경우 기본 type=book 추가
        if (path == "/home/util" && baseUri.getQueryParameter("type").isNullOrEmpty()) {
            urlBuilder.appendQueryParameter("type", "book")
        }

        urlBuilder.toString()
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
                    request?.url?.let { uri ->
                        Log.d("CustomWebView", "Intercepted URL: $uri")

                        // 1) 외부 도메인 체크
                        if (uri.host != "intip.inuappcenter.kr") {
                            Log.d("CustomWebView", "외부 도메인 -> 브라우저 열기: ${uri.host}")
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            context.startActivity(intent)
                            return true
                        }

                        // 2) path와 query 파라미터 처리
                        val rawPath = uri.path ?: ""
                        val cleanedPath = if (rawPath.startsWith("/app")) {
                            rawPath.removePrefix("/app")
                        } else {
                            rawPath
                        }

                        val queryId = uri.getQueryParameter("id")
                        val typeParam = uri.getQueryParameter("type")

                        Log.d(
                            "CustomWebView",
                            "HOST: ${uri.host}, RAW_PATH: $rawPath, CLEANED_PATH: $cleanedPath, ID: $queryId, TYPE: $typeParam"
                        )

                        // Write 페이지 특수 처리
                        if (cleanedPath == "/write") {
                            if (queryId != null) {
                                // 수정 페이지로 이동
                                navController.navigate(AllDestination.WriteEdit.createRoute(queryId))
                            } else {
                                // 새 글 작성 페이지로 이동
                                navController.navigate(AllDestination.Write.route)
                            }
                            return true
                        }

                        // Query Parameter 기반 라우팅 처리
                        when (cleanedPath) {
                            "/home/tips" -> {
                                when (typeParam) {
                                    "notice" -> navController.navigate(AllDestination.TipsNotice.route)
                                    else -> navController.navigate(AllDestination.Tips.route)
                                }
                                return true
                            }
                            "/home/council" -> {
                                when (typeParam) {
                                    "notice" -> navController.navigate(AllDestination.CouncilNotice.route)
                                    "petition" -> navController.navigate(AllDestination.CouncilPetition.route)
                                    "HelloBus" -> navController.navigate(AllDestination.CouncilHelloBus.route)
                                    else -> navController.navigate(AllDestination.CouncilNotice.route)
                                }
                                return true
                            }
                            "/home/util" -> {
                                when (typeParam) {
                                    "book" -> navController.navigate(AllDestination.UtilBook.route)
                                    "lost" -> navController.navigate(AllDestination.UtilLost.route)
                                    else -> navController.navigate(AllDestination.UtilBook.route)
                                }
                                return true
                            }
                        }

                        // [CASE A] 동적 라우트에 해당하는지 확인 (ex: /postdetail)
                        if (dynamicRoutesMap.containsKey(cleanedPath)) {
                            val destination = dynamicRoutesMap[cleanedPath]!!
                            if (queryId.isNullOrEmpty()) {
                                Log.w("CustomWebView", "동적 라우트인데 id가 없음 → 웹뷰로 처리")
                                return super.shouldOverrideUrlLoading(view, request)
                            }
                            // ex) /postdetail -> PostDetail
                            when (destination) {
                                is AllDestination.PostDetail -> {
                                    navController.navigate(destination.createRoute(queryId))
                                }
                                is AllDestination.CouncilNoticeDetail -> {
                                    navController.navigate(destination.createRoute(queryId))
                                }
                                is AllDestination.PetitionDetail -> {
                                    navController.navigate(destination.createRoute(queryId))
                                }
                                else -> {
                                    Log.w("CustomWebView", "Unknown dynamic destination: $destination")
                                }
                            }
                            return true
                        }

                        // [CASE B] 정적 라우트
                        val staticDestination = AllDestination.webViewPage.find { it.webPath == cleanedPath }
                        if (staticDestination != null) {
                            // ex) /home/tips?type=notice → tips 페이지 이동
                            navController.navigate(staticDestination.route)
                            Log.d("CustomWebView", "정적 라우트 이동 -> ${staticDestination.route}")
                            return true
                        }

                        // [CASE C] 매핑되지 않은 path
                        Log.d("CustomWebView", "매핑되지 않은 path -> 웹뷰에서 처리: $cleanedPath")
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
            }
        }

        // onDispose 시 WebView 자원 정리
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
    fun goBack() {
        Handler(Looper.getMainLooper()).post {
            navController.popBackStack()
        }
    }
}

private val dynamicRoutesMap: Map<String, AllDestination> = mapOf(
    "/postdetail" to AllDestination.PostDetail,
    "/councilnoticedetail" to AllDestination.CouncilNoticeDetail,
    "/petitiondetail" to AllDestination.PetitionDetail
    // 필요 시 더 추가
)

