package inu.appcenter.intip_android

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
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
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var backPreviewImage: ImageView
    private val bitmapMap = mutableMapOf<Int, Bitmap>()

    private var isSwiping = false
    private var isScrolling = false
    private var isReady = false
    private var lastHistoryIndex = -1
    private var isBackNavigating = false
    private var pendingNotificationUrl: String? = null // 알림 이동 경로 임시 보관

    private lateinit var backPressCallback: OnBackPressedCallback
    private val networkHelper by lazy { NetworkHelper(this) }
    private val downloadHelper by lazy { DownloadHelper(this) }
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    // 상수 최적화
    private companion object {
        const val CAPTURE_DELAY = 300L
        const val BITMAP_SCALE = 0.5f
        const val ANIMATION_DURATION = 350L
        const val MAX_BITMAP_CACHE_SIZE = 15
    }

    // 캡처 실행
    private val scrollCaptureRunnable = Runnable {
        if (!isScrolling && !isSwiping && isReady) {
            captureCurrentState(null, false)
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
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContentView(R.layout.activity_main)

        splashScreen.setKeepOnScreenCondition { !isReady }

        createNotificationChannel()
        setupViews()
        setupWebView()
        setupBackPressHandler()
        checkAndRequestPermissions()
        
        handleIntent(intent)
        logFcmToken()
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "일반 알림"
            val descriptionText = "앱의 기본적인 알림을 수신합니다."
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("fcm_default_channel", name, importance).apply {
                description = descriptionText
                enableLights(true)
                enableVibration(true)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun logFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM_TOKEN", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }
            val token = task.result
            Log.d("FCM_TOKEN", "Current token: $token")
            if (!token.isNullOrBlank()) {
                FcmTokenBridge.updateToken(this, token, webView)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val targetPath = intent.getStringExtra("TARGET_PATH")
        val isFromNotification = intent.hasExtra("google.message_id") || intent.hasExtra("google.sent_time") || targetPath != null

        if (isFromNotification) {
            val url = when {
                targetPath != null -> {
                    if (targetPath.startsWith("http")) targetPath 
                    else Constants.BASE_URL + if (targetPath.startsWith("/")) targetPath else "/$targetPath"
                }
                else -> Constants.BASE_URL + "/home/alert"
            }
            
            // 이미 웹뷰가 로드된 상태(백그라운드에서 복귀)라면 즉시 이동
            if (webView.url != null) {
                webView.loadUrl(url)
            } else {
                // 앱이 처음 켜지는 상태라면 히스토리를 위해 홈을 먼저 로드하고 경로 보관
                pendingNotificationUrl = url
                loadInitialPage()
            }
        } else if (webView.url == null) {
            loadInitialPage()
        }
    }

    private fun setupViews() {
        webView = findViewById(R.id.webview)
        backPreviewImage = findViewById(R.id.back_preview)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        FcmTokenBridge.attachWebView(webView)

        webView.apply {
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
        }

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            setGeolocationEnabled(true)
            useWideViewPort = true
            loadWithOverviewMode = true
            textZoom = 100
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            userAgentString += Constants.USER_AGENT_SUFFIX
            setRenderPriority(WebSettings.RenderPriority.HIGH)
            cacheMode = WebSettings.LOAD_DEFAULT
        }

        webView.setOnScrollChangeListener { _, _, _, _, _ ->
            isScrolling = true
            webView.removeCallbacks(scrollCaptureRunnable)
            webView.postDelayed({
                isScrolling = false
                if (isReady && !isSwiping) {
                    webView.postDelayed(scrollCaptureRunnable, CAPTURE_DELAY)
                }
            }, 200)
        }

        webView.webViewClient = AppWebViewClient(
            context = this,
            onPageStartedCallback = { url ->
                updateBackPressState(url)
                webView.removeCallbacks(scrollCaptureRunnable)
                if (!isSwiping && lastHistoryIndex != -1) {
                    captureCurrentState(lastHistoryIndex, true)
                }
                if (isReady && !isSwiping) {
                    freezeCurrentScreen(url)
                    isReady = false
                }
            },
            onPageFinishedCallback = { url ->
                updateBackPressState(url)
                
                if (isBackNavigating) {
                    webView.alpha = 1f
                    webView.visibility = View.VISIBLE
                    fadeOutPreviewImage()
                    isBackNavigating = false
                } else {
                    playForwardAnimation()
                }
                
                // 홈 로딩이 끝난 시점에 대기 중인 알림 경로가 있다면 이동 (히스토리 스택 생성 완료)
                pendingNotificationUrl?.let { notificationUrl ->
                    pendingNotificationUrl = null
                    webView.loadUrl(notificationUrl)
                }
                
                isReady = true
                webView.postDelayed(scrollCaptureRunnable, CAPTURE_DELAY)
                lastHistoryIndex = webView.copyBackForwardList().currentIndex
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

    private fun captureCurrentState(targetIndex: Int? = null, isSync: Boolean = false) {
        if (webView.width <= 0 || webView.height <= 0 || isSwiping) return
        if (!isSync && (!isReady || isScrolling)) return

        val list = webView.copyBackForwardList()
        val indexToSave = targetIndex ?: list.currentIndex
        if (indexToSave < 0) return

        try {
            val width = (webView.width * BITMAP_SCALE).toInt()
            val height = (webView.height * BITMAP_SCALE).toInt()
            if (width <= 0 || height <= 0) return
            
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

            if (!isSync && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val location = IntArray(2)
                webView.getLocationInWindow(location)
                val rect = Rect(location[0], location[1], location[0] + webView.width, location[1] + webView.height)

                PixelCopy.request(window, rect, bitmap, { copyResult ->
                    if (copyResult == PixelCopy.SUCCESS) updateBitmapMap(indexToSave, bitmap)
                    else bitmap.recycle()
                }, Handler(Looper.getMainLooper()))
            } else {
                val canvas = Canvas(bitmap)
                canvas.scale(BITMAP_SCALE, BITMAP_SCALE)
                canvas.translate(-webView.scrollX.toFloat(), -webView.scrollY.toFloat())
                webView.draw(canvas)
                updateBitmapMap(indexToSave, bitmap)
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun updateBitmapMap(indexToSave: Int, bitmap: Bitmap) {
        val oldBitmap = bitmapMap[indexToSave]
        if (oldBitmap != null && !oldBitmap.isRecycled) {
            if (backPreviewImage.visibility == View.VISIBLE && backPreviewImage.drawable != null) {
                bitmap.recycle()
                return
            }
            oldBitmap.recycle()
        }
        bitmapMap[indexToSave] = bitmap

        if (bitmapMap.size > MAX_BITMAP_CACHE_SIZE) {
            val currentIndex = webView.copyBackForwardList().currentIndex
            val keyToRemove = bitmapMap.keys.maxByOrNull { abs(it - currentIndex) }
            if (keyToRemove != null && abs(keyToRemove - currentIndex) > 5) {
                bitmapMap.remove(keyToRemove)?.let { if (!it.isRecycled) it.recycle() }
            }
        }
    }

    private fun playForwardAnimation() {
        if (isSwiping) return
        val list = webView.copyBackForwardList()
        val currentIndex = list.currentIndex
        val screenWidth = webView.width.toFloat()
        val currentUrl = webView.url ?: ""
        val currentPath = currentUrl.replace(Constants.BASE_URL, "")
        val isHomePath = currentPath in Constants.RESTRICTED_PATHS || currentPath.isEmpty() || currentPath == "/"

        if (!isHomePath && lastHistoryIndex != -1 && currentIndex > lastHistoryIndex && screenWidth > 0) {
            val prevBitmap = bitmapMap[currentIndex - 1]
            if (prevBitmap != null && !prevBitmap.isRecycled) {
                val interpolator = PathInterpolator(0.22f, 1f, 0.36f, 1f)
                backPreviewImage.apply {
                    setImageBitmap(prevBitmap)
                    visibility = View.VISIBLE
                    alpha = 1f
                    translationX = 0f
                    animate().translationX(-screenWidth * 0.3f).alpha(0.8f).setDuration(ANIMATION_DURATION).setInterpolator(interpolator).start()
                }
                webView.apply {
                    translationX = screenWidth
                    elevation = 20f
                    animate().translationX(0f).setDuration(ANIMATION_DURATION).setInterpolator(interpolator).withEndAction {
                        elevation = 0f
                        fadeOutPreviewImage()
                    }.start()
                }
            }
        } else {
            backPreviewImage.setImageBitmap(null)
            webView.translationX = 0f
            fadeOutPreviewImage()
        }
    }

    private fun freezeCurrentScreen(targetUrl: String?) {
        if (isSwiping) return
        val list = webView.copyBackForwardList()
        val targetIndex = list.currentIndex
        if (targetIndex < 0) return

        val targetPath = targetUrl?.replace(Constants.BASE_URL, "") ?: ""
        if (targetPath !in Constants.RESTRICTED_PATHS && targetPath.isNotEmpty()) {
            bitmapMap[targetIndex]?.let {
                if (!it.isRecycled) {
                    backPreviewImage.setImageBitmap(it)
                    backPreviewImage.visibility = View.VISIBLE
                    backPreviewImage.alpha = 1f
                    backPreviewImage.translationX = 0f
                    webView.translationX = webView.width.toFloat()
                }
            }
        }
    }

    private fun setupBackPressHandler() {
        backPressCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackStarted(backEvent: BackEventCompat) {
                isSwiping = true
                val currentIndex = webView.copyBackForwardList().currentIndex
                if (bitmapMap[currentIndex - 1] == null) captureCurrentState(currentIndex, true)
                
                val prevBitmap = bitmapMap[currentIndex - 1]
                if (prevBitmap != null && !prevBitmap.isRecycled) {
                    backPreviewImage.setImageBitmap(prevBitmap)
                    backPreviewImage.visibility = View.VISIBLE
                    backPreviewImage.alpha = 1f
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
                isBackNavigating = true
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

                backPreviewImage.animate().translationX(0f).alpha(1f).setDuration(250).setInterpolator(interpolator).start()

                if (webView.canGoBack()) {
                    webView.goBack()
                    webView.postDelayed({ if(isBackNavigating) fadeOutPreviewImage() }, 500)
                } else {
                    fadeOutPreviewImage()
                }
            }

            override fun handleOnBackCancelled() {
                isSwiping = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) webView.clipToOutline = false
                webView.animate().translationX(0f).setDuration(300).setInterpolator(PathInterpolator(0.22f, 1f, 0.36f, 1f)).start()
                backPreviewImage.animate().alpha(0f).setDuration(250).withEndAction { 
                    if (!isSwiping) backPreviewImage.visibility = View.INVISIBLE
                }.start()
            }
        }
        onBackPressedDispatcher.addCallback(this, backPressCallback)
    }

    private fun fadeOutPreviewImage() {
        if (backPreviewImage.visibility == View.VISIBLE) {
            backPreviewImage.animate().alpha(0f).setDuration(300).withEndAction {
                if (!isSwiping) {
                    backPreviewImage.visibility = View.INVISIBLE
                    backPreviewImage.alpha = 1f
                    backPreviewImage.setImageBitmap(null)
                }
                isBackNavigating = false
            }.start()
        }
    }

    private fun loadInitialPage() {
        if (networkHelper.isInternetAvailable()) webView.loadUrl(Constants.BASE_URL)
        else {
            isReady = true
            showRetryDialog()
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) pickMultipleImagesLauncher.launch(request)
        else pickImageLauncher.launch(request)
        return true
    }

    private fun handleFileSelection(uris: List<Uri>?) {
        val result = uris?.map { FileUtil.copyUriToCache(this, it) }?.toTypedArray()
        filePathCallback?.onReceiveValue(result)
        filePathCallback = null
    }

    private fun showToast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    private fun showRetryDialog() {
        AlertDialog.Builder(this).setTitle("인터넷 연결 없음").setMessage("연결 후 다시 시도해주세요.").setPositiveButton("재시도") { _, _ -> loadInitialPage() }.setNegativeButton("종료") { _, _ -> finish() }.setCancelable(false).show()
    }

    private fun showRefreshDialog() {
        AlertDialog.Builder(this).setTitle("업데이트").setMessage("최신 버전으로 업데이트할까요?").setPositiveButton("확인") { _, _ ->
            webView.clearCache(true)
            webView.reload()
        }.setNegativeButton("취소", null).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        FcmTokenBridge.detachWebView(webView)
        webView.removeCallbacks(scrollCaptureRunnable)
        bitmapMap.values.forEach { if (!it.isRecycled) it.recycle() }
        bitmapMap.clear()
    }
}
