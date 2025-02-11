package inu.appcenter.intip_android.ui.component

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.JsResult
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import inu.appcenter.intip_android.ui.login.AuthViewModel
import inu.appcenter.intip_android.ui.navigate.AllDestination
import inu.appcenter.intip_android.utils.K
import java.io.File
import java.io.UnsupportedEncodingException

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CustomAndroidView(
    modifier: Modifier = Modifier,
    path: String,
    token: String? = null,
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val context = LocalContext.current
    val webBaseUrl = K.WEB_BASE_URL

    var filePathCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

    val pickFileLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
            val result = if (uris.isNotEmpty()) uris.toTypedArray() else null
            filePathCallback?.onReceiveValue(result)
            filePathCallback = null
        }

    // Handle back press for WebView
    BackHandler {
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        if (currentRoute == AllDestination.Home.route) {
            // If on Home screen, exit the app
            (context as? Activity)?.finishAffinity()
        } else {
            // Normal back navigation
            navController.popBackStack()
        }
    }

    // URL과 쿼리 파라미터 처리
    val url = try {
        val baseUri = Uri.parse("$webBaseUrl$path")
        val urlBuilder = baseUri.buildUpon()

        // 토큰이 있다면 추가
        token?.let {
            urlBuilder.appendQueryParameter("token", it)
        }

        // council 페이지의 경우 기본 type=notice 추가
        if (path == "/home/council" && baseUri.getQueryParameter("type").isNullOrEmpty()) {
            urlBuilder.appendQueryParameter("type", "notice")
        }
        // util 페이지의 경우 기본 type=book 추가
        if (path == "/home/util" && baseUri.getQueryParameter("type").isNullOrEmpty()) {
            urlBuilder.appendQueryParameter("type", "book")
        }
        // campus 페이지의 경우 기본 type=campusmap 추가
        if (path == "/home/campus" && baseUri.getQueryParameter("type").isNullOrEmpty()) {
            urlBuilder.appendQueryParameter("type", "campusmap")
        }
        urlBuilder.toString()
    } catch (e: UnsupportedEncodingException) {
        Log.e("CustomAndroidView", "URL 인코딩 에러: ${e.message}")
        "$webBaseUrl$path"
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

            setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                Log.d("DownloadListener", "Download URL: $url")
                if (url.startsWith("data:")) {
                    // data URI인 경우 직접 파일 저장
                    try {
                        // data URI 형식: data:[<mediatype>][;base64],<data>
                        val commaIndex = url.indexOf(',')
                        if (commaIndex == -1) {
                            Log.e("DownloadListener", "잘못된 data URI")
                            return@setDownloadListener
                        }
                        val meta = url.substring(5, commaIndex) // "data:" 이후부터 콤마 전까지
                        val dataString = url.substring(commaIndex + 1)
                        val isBase64 = meta.endsWith(";base64")
                        val fileData: ByteArray = if (isBase64) {
                            Base64.decode(dataString, Base64.DEFAULT)
                        } else {
                            dataString.toByteArray(Charsets.UTF_8)
                        }

                        // MIME 타입과 확장자 추출
                        val actualMimeType =
                            meta.split(";").firstOrNull() ?: "application/octet-stream"
                        val extension = MimeTypeMap.getSingleton()
                            .getExtensionFromMimeType(actualMimeType) ?: "bin"
                        val fileName =
                            URLUtil.guessFileName(url, contentDisposition, actualMimeType)
                                .ifBlank { "downloaded_file.$extension" }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            // Android Q 이상: MediaStore API를 사용하여 Pictures 폴더에 저장 (갤러리 자동 등록)
                            val values = ContentValues().apply {
                                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                                put(MediaStore.Images.Media.MIME_TYPE, actualMimeType)
                                put(
                                    MediaStore.Images.Media.RELATIVE_PATH,
                                    Environment.DIRECTORY_PICTURES
                                )
                            }
                            val uri = context.contentResolver.insert(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                values
                            )
                            if (uri != null) {
                                context.contentResolver.openOutputStream(uri).use { output ->
                                    output?.write(fileData)
                                }
                                Toast.makeText(
                                    context,
                                    "갤러리에 저장되었습니다: $fileName",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                Toast.makeText(context, "갤러리 저장 실패", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            // Android 9(API 28) 이하: Downloads 폴더에 저장 후 MediaScannerConnection.scanFile() 호출
                            val downloadsDir =
                                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                            val file = File(downloadsDir, fileName)
                            file.outputStream().use { output ->
                                output.write(fileData)
                            }
                            MediaScannerConnection.scanFile(
                                context,
                                arrayOf(file.absolutePath),
                                arrayOf(actualMimeType)
                            ) { path, uri ->
                                Log.d("MediaScanner", "Scanned $path : $uri")
                            }
                            Toast.makeText(
                                context,
                                "파일이 다운로드되었습니다: ${file.absolutePath}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } catch (e: Exception) {
                        Log.e("DownloadListener", "data URI 파일 저장 실패: ${e.message}")
                        Toast.makeText(context, "파일 다운로드 실패", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // HTTP/HTTPS URL인 경우 DownloadManager를 사용
                    val downloadManager =
                        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
                    val request = DownloadManager.Request(Uri.parse(url)).apply {
                        addRequestHeader("User-Agent", userAgent)
                        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        setTitle(fileName)
                        setDescription("다운로드 중...")
                        setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                    }
                    downloadManager.enqueue(request)
                }
            }


            // JavaScript Interface 추가
            addJavascriptInterface(AndroidBridge(navController, authViewModel), "AndroidBridge")

            webChromeClient = object : WebChromeClient() {
                override fun onJsAlert(
                    view: WebView?,
                    url: String?,
                    message: String?,
                    result: JsResult?
                ): Boolean {
                    if (message?.contains("로그인 정보가 만료되었습니다") == true ||
                        message?.contains("다시 로그인해 주세요.") == true
                    ) {
                        if (!authViewModel.uiState.value.hasShownTokenAlert) {
                            authViewModel.setTokenAlertShown()
                        }
                        result?.confirm()
                        return true
                    }
                    return super.onJsAlert(view, url, message, result)
                }

                override fun onShowFileChooser(
                    webView: WebView?,
                    callback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
                ): Boolean {
                    filePathCallback?.onReceiveValue(null)
                    filePathCallback = callback

                    pickFileLauncher.launch(arrayOf("image/*"))
                    return true
                }
            }

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
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

                        // 로그인 페이지 처리 추가
                        if (cleanedPath == "/login") {
                            Log.d("CustomWebView", "로그인 페이지 이동")
                            navController.navigate(AllDestination.Login.route)
                            return true
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
                                when {
                                    typeParam == "notice" -> navController.navigate(AllDestination.TipsNotice.route)
                                    !uri.getQueryParameter("search").isNullOrEmpty() -> {
                                        val searchQuery = uri.getQueryParameter("search") ?: ""
                                        navController.navigate(
                                            AllDestination.TipsSearch.createRoute(
                                                searchQuery
                                            )
                                        )
                                    }

                                    else -> navController.navigate(AllDestination.Tips.route)
                                }
                                return true
                            }

                            "/home/council" -> {
                                when (typeParam) {
                                    "notice" -> navController.navigate(AllDestination.CouncilNotice.route)
                                    "petition" -> navController.navigate(AllDestination.CouncilPetition.route)
                                    else -> navController.navigate(AllDestination.CouncilNotice.route)
                                }
                                return true
                            }

                            "/home/campus" -> {
                                when (typeParam) {
                                    "campusmap" -> navController.navigate(AllDestination.CampusMap.route)
                                    "HelloBus" -> navController.navigate(AllDestination.CampusHelloBus.route)
                                    else -> navController.navigate(AllDestination.CampusMap.route)
                                }
                                return true
                            }

                            "/home/util" -> {
                                when (typeParam) {
                                    "book" -> navController.navigate(AllDestination.UtilBook.route)
                                    "lost" -> navController.navigate(AllDestination.UtilLost.route)
                                    "rental" -> navController.navigate(AllDestination.UtilRentals.route)
                                    else -> navController.navigate(AllDestination.UtilBook.route)
                                }
                                return true
                            }
                        }

                        // [CASE A] 동적 라우트에 해당하는지 확인 (ex: /postdetail)
                        val dynamicDestination = dynamicRoutesMap[cleanedPath]
                        if (dynamicDestination != null) {
                            if (queryId.isNullOrEmpty()) {
                                Log.w("CustomWebView", "동적 라우트인데 id가 없음 → 웹뷰로 처리")
                                return super.shouldOverrideUrlLoading(view, request)
                            }
                            when (dynamicDestination) {
                                is AllDestination.PostDetail -> {
                                    navController.navigate(dynamicDestination.createRoute(queryId))
                                }

                                is AllDestination.CouncilNoticeDetail -> {
                                    navController.navigate(dynamicDestination.createRoute(queryId))
                                }

                                is AllDestination.PetitionDetail -> {
                                    navController.navigate(dynamicDestination.createRoute(queryId))
                                }

                                else -> {
                                    Log.w(
                                        "CustomWebView",
                                        "Unknown dynamic destination: $dynamicDestination"
                                    )
                                }
                            }
                            return true
                        }

                        // [CASE B] 정적 라우트
                        val staticDestination =
                            AllDestination.webViewPage.find { it.webPath == cleanedPath }
                        if (staticDestination != null) {
                            val currentRoute =
                                navController.currentBackStackEntry?.destination?.route
                            if (staticDestination.route != currentRoute) {
                                navController.navigate(staticDestination.route) {
                                    launchSingleTop = true
                                    restoreState = true
                                }
                                Log.d("CustomWebView", "정적 라우트 이동 -> ${staticDestination.route}")
                            }
                            return true
                        }

                        val destination =
                            AllDestination.webViewPage.find { it.webPath == cleanedPath }
                        destination?.let {
                            navController.navigate(it.route)
                        }

                        Log.d("CustomWebView", "매핑되지 않은 path -> 웹뷰에서 처리: $cleanedPath")
                        return super.shouldOverrideUrlLoading(view, request)
                    }
                    return super.shouldOverrideUrlLoading(view, request)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    Log.d("CustomAndroidView", "onPageFinished: $url")
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
        modifier = modifier.fillMaxSize(),
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
class AndroidBridge(
    private val navController: NavController,
    private val authViewModel: AuthViewModel
) {
    @JavascriptInterface
    fun goBack() {
        Handler(Looper.getMainLooper()).post {
            navController.popBackStack()
        }
    }

    @JavascriptInterface
    fun handleLogout() {
        Handler(Looper.getMainLooper()).post {
            authViewModel.logout()
            navController.navigate(AllDestination.Home.route) {
                popUpTo(AllDestination.Home.route) {
                    inclusive = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }
}

private val dynamicRoutesMap: Map<String, AllDestination> = mapOf(
    "/postdetail" to AllDestination.PostDetail,
    "/councilnoticedetail" to AllDestination.CouncilNoticeDetail,
    "/petitiondetail" to AllDestination.PetitionDetail
    // 필요 시 더 추가
)
