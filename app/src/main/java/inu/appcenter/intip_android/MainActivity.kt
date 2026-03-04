package inu.appcenter.intip_android

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.PixelCopy
import android.view.View
import android.view.ViewOutlineProvider
import android.view.animation.PathInterpolator
import android.webkit.ValueCallback
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.firebase.FirebaseApp

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var backPreviewImage: ImageView
    private val bitmapMap = mutableMapOf<Int, Bitmap>()

    private var isSwiping = false
    private var isScrolling = false
    private var isReady = false
    private var lastHistoryIndex = -1

    private lateinit var backPressCallback: OnBackPressedCallback
    private val networkHelper by lazy { NetworkHelper(this) }
    private val downloadHelper by lazy { DownloadHelper(this) }
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    // 상수
    private companion object {
        const val CAPTURE_DELAY = 300L
        const val BITMAP_SCALE = 1f
        const val ANIMATION_DURATION = 350L
    }

    // 캡처 실행
    private val scrollCaptureRunnable = Runnable {
        if (!isScrolling && !isSwiping && isReady) {
            captureCurrentStateSilently()
        }
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        handleFileSelection(uri?.let { listOf(it) })
    }

    private val pickMultipleImagesLauncher = registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(5)) { uris ->
        handleFileSelection(uris)
    }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->

            results.filterValues { !it }.keys.forEach { permission ->
                Log.d("Permission", "$permission 권한이 거부되었습니다.")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContentView(R.layout.activity_main)

        splashScreen.setKeepOnScreenCondition { !isReady }

        setupViews()
        setupWebView()
        setupBackPressHandler()
        checkAndRequestPermissions()
        loadInitialPage()
    }

    private fun setupViews() {
        webView = findViewById(R.id.webview)
        backPreviewImage = findViewById(R.id.back_preview)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.apply {
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            // 하드웨어 가속
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
        }

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            textZoom = 100
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            userAgentString += Constants.USER_AGENT_SUFFIX
            // 렌더링 최적화
            setRenderPriority(WebSettings.RenderPriority.HIGH)
            cacheMode = WebSettings.LOAD_DEFAULT
        }

        // 스크롤 리스너
        webView.setOnScrollChangeListener { _, _, _, _, _ ->
            isScrolling = true
            webView.removeCallbacks(scrollCaptureRunnable)

            // 스크롤 종료 감지
            webView.postDelayed({
                isScrolling = false
                if (isReady && !isSwiping) {
                    webView.postDelayed(scrollCaptureRunnable, CAPTURE_DELAY)
                }
            }, 150)
        }

        webView.webViewClient = AppWebViewClient(
            context = this,
            onPageStartedCallback = { url ->
                updateBackPressState(url)
                webView.removeCallbacks(scrollCaptureRunnable)
                if (isReady && !isSwiping) {
                    freezeCurrentScreen(url)
                    isReady = false
                }
            },
            onPageFinishedCallback = { url ->
                updateBackPressState(url)
                playForwardAnimation()
                isReady = true
                // 로딩 후 지연 캡처
                webView.postDelayed(scrollCaptureRunnable, CAPTURE_DELAY)
            }
        )

        webView.webChromeClient = AppWebChromeClient(this) { callback, _ ->
            handleShowFileChooser(callback)
        }

        webView.addJavascriptInterface(WebAppInterface {
            runOnUiThread { showRefreshDialog() }
        }, "AndroidBridge")

        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            downloadHelper.downloadFile(url, userAgent, contentDisposition, mimeType)
        }
    }

    private fun captureCurrentStateSilently() {
        // 실행 조건 검사
        if (isSwiping || isScrolling || !isReady || webView.width <= 0 || webView.height <= 0) return

        val list = webView.copyBackForwardList()
        val indexToSave = list.currentIndex
        if (indexToSave < 0) return

        try {
            val width = (webView.width * BITMAP_SCALE).toInt()
            val height = (webView.height * BITMAP_SCALE).toInt()
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // 비동기 캡처
                val location = IntArray(2)
                webView.getLocationInWindow(location)
                val rect = Rect(
                    location[0],
                    location[1],
                    location[0] + webView.width,
                    location[1] + webView.height
                )

                PixelCopy.request(
                    window,
                    rect,
                    bitmap,
                    { copyResult ->
                        if (copyResult == PixelCopy.SUCCESS) {
                            updateBitmapMap(indexToSave, bitmap)
                        } else {
                            bitmap.recycle()
                        }
                    },
                    Handler(Looper.getMainLooper())
                )
            } else {
                // 동기 캡처
                val canvas = Canvas(bitmap)
                canvas.scale(BITMAP_SCALE, BITMAP_SCALE)
                canvas.translate(-webView.scrollX.toFloat(), -webView.scrollY.toFloat())
                webView.draw(canvas)

                updateBitmapMap(indexToSave, bitmap)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 맵 갱신 및 메모리 정리
    private fun updateBitmapMap(indexToSave: Int, bitmap: Bitmap) {
        bitmapMap[indexToSave]?.recycle()
        bitmapMap[indexToSave] = bitmap

        if (bitmapMap.size > 5) {
            bitmapMap.keys.minOrNull()?.let { key ->
                if (key < indexToSave - 1) bitmapMap.remove(key)?.recycle()
            }
        }
    }

    private fun playForwardAnimation() {
        if (isSwiping) return

        val list = webView.copyBackForwardList()
        val currentIndex = list.currentIndex
        val currentUrl = webView.url ?: ""
        val currentPath = currentUrl.replace(Constants.BASE_URL, "")
        val screenWidth = webView.width.toFloat()

        val isHomePath = currentPath in Constants.RESTRICTED_PATHS || currentPath.isEmpty() || currentPath == "/"
        val prevBitmap = bitmapMap[currentIndex - 1]

        if (!isHomePath && lastHistoryIndex != -1 && currentIndex > lastHistoryIndex && prevBitmap != null && screenWidth > 0) {
            val interpolator = PathInterpolator(0.22f, 1f, 0.36f, 1f)

            backPreviewImage.apply {
                setImageBitmap(prevBitmap)
                visibility = View.VISIBLE
                alpha = 1f
                translationX = 0f
                animate()
                    .translationX(-screenWidth * 0.3f)
                    .alpha(0.8f)
                    .setDuration(ANIMATION_DURATION)
                    .setInterpolator(interpolator)
                    .start()
            }

            webView.apply {
                translationX = screenWidth
                elevation = 20f
                animate()
                    .translationX(0f)
                    .setDuration(ANIMATION_DURATION)
                    .setInterpolator(interpolator)
                    .withEndAction {
                        elevation = 0f
                        fadeOutPreviewImage()
                    }
                    .start()
            }
        } else {
            webView.translationX = 0f
            fadeOutPreviewImage()
        }
        lastHistoryIndex = currentIndex
    }

    private fun freezeCurrentScreen(targetUrl: String?) {
        if (isSwiping) return
        val list = webView.copyBackForwardList()
        val targetIndex = list.currentIndex
        if (targetIndex < 0) return

        val targetPath = targetUrl?.replace(Constants.BASE_URL, "") ?: ""
        if (targetPath !in Constants.RESTRICTED_PATHS && targetPath.isNotEmpty()) {
            bitmapMap[targetIndex]?.let {
                backPreviewImage.setImageBitmap(it)
                backPreviewImage.visibility = View.VISIBLE
                backPreviewImage.alpha = 1f
                backPreviewImage.translationX = 0f
                webView.translationX = webView.width.toFloat()
            }
        }
    }

    private fun setupBackPressHandler() {
        backPressCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackStarted(backEvent: BackEventCompat) {
                isSwiping = true
                val currentIndex = webView.copyBackForwardList().currentIndex
                bitmapMap[currentIndex - 1]?.let {
                    backPreviewImage.setImageBitmap(it)
                    backPreviewImage.visibility = View.VISIBLE
                }
                webView.pivotX = if (backEvent.swipeEdge == BackEventCompat.EDGE_LEFT) 0f else webView.width.toFloat()
            }

            override fun handleOnBackProgressed(backEvent: BackEventCompat) {
                val progress = backEvent.progress
                val screenWidth = webView.width.toFloat()

                webView.translationX = progress * screenWidth
                backPreviewImage.apply {
                    translationX = -screenWidth * 0.2f * (1f - progress)
                    alpha = 0.7f + (progress * 0.3f)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    webView.clipToOutline = true
                    webView.outlineProvider = object : ViewOutlineProvider() {
                        override fun getOutline(view: View, outline: android.graphics.Outline) {
                            outline.setRoundRect(0, 0, view.width, view.height, progress * 30f)
                        }
                    }
                }
            }

            override fun handleOnBackPressed() {
                isSwiping = false
                val screenWidth = webView.width.toFloat()
                val interpolator = PathInterpolator(0.22f, 1f, 0.36f, 1f)

                webView.animate()
                    .translationX(screenWidth)
                    .alpha(0f)
                    .setDuration(250)
                    .setInterpolator(interpolator)
                    .withEndAction {
                        webView.translationX = 0f
                        webView.alpha = 1f
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) webView.clipToOutline = false
                    }
                    .start()

                backPreviewImage.animate()
                    .translationX(0f)
                    .alpha(1f)
                    .setDuration(250)
                    .setInterpolator(interpolator)
                    .start()

                if (webView.canGoBack()) {
                    webView.goBack()
                    webView.postDelayed({ fadeOutPreviewImage() }, 300)
                } else {
                    fadeOutPreviewImage()
                }
            }

            override fun handleOnBackCancelled() {
                isSwiping = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) webView.clipToOutline = false

                webView.animate()
                    .translationX(0f)
                    .setDuration(300)
                    .setInterpolator(PathInterpolator(0.22f, 1f, 0.36f, 1f))
                    .start()

                backPreviewImage.animate()
                    .alpha(0f)
                    .setDuration(250)
                    .withEndAction { backPreviewImage.visibility = View.INVISIBLE }
                    .start()
            }
        }
        onBackPressedDispatcher.addCallback(this, backPressCallback)
    }

    private fun fadeOutPreviewImage() {
        if (backPreviewImage.visibility == View.VISIBLE) {
            backPreviewImage.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    backPreviewImage.visibility = View.INVISIBLE
                    backPreviewImage.alpha = 1f
                }
                .start()
        }
    }

    private fun loadInitialPage() {
        if (networkHelper.isInternetAvailable()) {
            webView.loadUrl(Constants.BASE_URL)
        } else {
            isReady = true
            showRetryDialog()
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(Manifest.permission.CAMERA).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    private fun updateBackPressState(url: String?) {
        val currentPath = url?.replace(Constants.BASE_URL, "") ?: ""
        val isRootPath = currentPath in Constants.RESTRICTED_PATHS || currentPath.isEmpty()
        backPressCallback.isEnabled = webView.canGoBack() && !isRootPath
    }

    private fun handleShowFileChooser(callback: ValueCallback<Array<Uri>>?): Boolean {
        filePathCallback?.onReceiveValue(null)
        filePathCallback = callback
        val request = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            pickMultipleImagesLauncher.launch(request)
        } else {
            pickImageLauncher.launch(request)
        }
        return true
    }

    private fun handleFileSelection(uris: List<Uri>?) {
        val result = uris?.map { FileUtil.copyUriToCache(this, it) }?.toTypedArray()
        filePathCallback?.onReceiveValue(result)
        filePathCallback = null
    }

    private fun showToast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    private fun showRetryDialog() {
        AlertDialog.Builder(this)
            .setTitle("인터넷 연결 없음")
            .setMessage("연결 후 다시 시도해주세요.")
            .setPositiveButton("재시도") { _, _ -> loadInitialPage() }
            .setNegativeButton("종료") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    private fun showRefreshDialog() {
        AlertDialog.Builder(this)
            .setTitle("업데이트")
            .setMessage("최신 버전으로 업데이트할까요?")
            .setPositiveButton("확인") { _, _ ->
                webView.clearCache(true)
                webView.reload()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        webView.removeCallbacks(scrollCaptureRunnable)
        bitmapMap.values.forEach { it.recycle() }
        bitmapMap.clear()
    }
}