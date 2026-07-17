package inu.appcenter.intip_android

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
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
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var backPreviewImage: ImageView
    private val bitmapMap = mutableMapOf<Int, Bitmap>()

    private var isSwiping = false
    private var isReady = false
    private var currentHistoryIndex = -1
    private var isBackNavigating = false
    private var pendingForwardPreviewIndex: Int? = null
    private var awaitingVisualCommit = false
    private var hasCompletedLaunchRefresh = false
    private var hasIssuedLaunchReload = false
    private var hasLoggedPostRefreshWebDiagnostics = false
    private var isAwaitingLaunchWebCleanup = false
    private var isManualRefreshInProgress = false
    private var deferredStartupIntent: Intent? = null
    private var isCapturingBeforeNavigation = false
    private var pendingCapturedNavigationUrl: String? = null
    private var isBackAnimationFinished = false
    private var isBackTargetVisualReady = false
    private var isAwaitingBackVisualState = false
    private var nextVisualStateRequestId = 1L
    private var nextCaptureRequestId = 1L
    private var lastCapturedIndex = -1
    private var lastCapturedUrl: String? = null
    private var lastCapturedScrollX = Int.MIN_VALUE
    private var lastCapturedScrollY = Int.MIN_VALUE
    private val latestCaptureRequestIds = mutableMapOf<Int, Long>()
    private var pendingNotificationUrl: String? = null // 알림 이동 경로 임시 보관

    private lateinit var backPressCallback: OnBackPressedCallback
    private val networkHelper by lazy { NetworkHelper(this) }
    private val downloadHelper by lazy { DownloadHelper(this) }
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    // 상수 최적화
    private companion object {
        const val WEBVIEW_REFRESH_TAG = "WebViewRefresh"
        const val BITMAP_SCALE = 0.5f
        const val ANIMATION_DURATION = 350L
        const val BACK_CROSSFADE_DURATION = 220L
        const val BACK_CROSSFADE_PREVIEW_DELAY = 24L
        const val BACK_CROSSFADE_WEBVIEW_START_ALPHA = 0.12f
        const val MAX_BITMAP_CACHE_SIZE = 15
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
        logWebViewEnvironment()
        setupBackPressHandler()
        checkAndRequestPermissions()

        startLaunchRefresh()
        handleIntent(intent)
        logFcmToken()
    }

    override fun onResume() {
        super.onResume()
        // 포그라운드로 돌아올 때마다 Play Services 가용성 체크 (정석 구현)
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
        val availability = GoogleApiAvailability.getInstance()
        
        // makeGooglePlayServicesAvailable: 가용하지 않을 경우 표준 해결 UI(업데이트 등)를 시도하고 결과를 Task로 반환 (정석)
        availability.makeGooglePlayServicesAvailable(this).addOnCompleteListener { task: com.google.android.gms.tasks.Task<Void?> ->
            if (task.isSuccessful) {
                // Play Services가 준비된 상태에서만 토큰 요청
                fetchAndLogFcmToken()
            } else {
                val resultCode = availability.isGooglePlayServicesAvailable(this)
                Log.w("FCM_TOKEN", "Google Play Services를 사용할 수 없습니다. 코드: $resultCode", task.exception)
            }
        }
    }

    private fun fetchAndLogFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM_TOKEN", "FCM 등록 토큰을 가져오지 못했습니다", task.exception)
                return@addOnCompleteListener
            }
            val token = task.result
            Log.d("FCM_TOKEN", "현재 토큰: $token")
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

    private fun handleIntent(intent: Intent): Boolean {
        if (!hasCompletedLaunchRefresh) {
            Log.d(WEBVIEW_REFRESH_TAG, "시작 새로고침이 끝날 때까지 handleIntent를 보류합니다")
            deferredStartupIntent = intent
            return false
        }

        val targetPath = intent.getStringExtra("TARGET_PATH")
        val isFromNotification = intent.hasExtra("google.message_id") || intent.hasExtra("google.sent_time") || targetPath != null
        Log.d(
            WEBVIEW_REFRESH_TAG,
            "시작 새로고침 후 handleIntent 실행: 알림유입=$isFromNotification, targetPath=$targetPath, 현재URL=${webView.url}"
        )

        if (isFromNotification && targetPath != null) {
            val url = if (targetPath.startsWith("http")) targetPath 
                      else Constants.BASE_URL + if (targetPath.startsWith("/")) targetPath else "/$targetPath"
            
            // 이미 웹뷰가 로드된 상태(백그라운드에서 복귀)라면 즉시 이동
            if (webView.url != null) {
                webView.loadUrl(url)
                return true
            } else {
                // 앱이 처음 켜지는 상태라면 히스토리를 위해 홈을 먼저 로드하고 경로 보관
                pendingNotificationUrl = url
                loadInitialPage()
                return true
            }
        } else if (webView.url == null) {
            loadInitialPage()
            return true
        }

        return false
    }

    private fun setupViews() {
        webView = findViewById(R.id.webview)
        backPreviewImage = findViewById(R.id.back_preview)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        FcmTokenBridge.attachWebView(webView)

        webView.apply {
            setBackgroundColor(this@MainActivity.getColor(R.color.app_background_color))
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

        webView.webViewClient = AppWebViewClient(
            context = this,
            onPageStartedCallback = { url ->
                updateBackPressState(url)
                
                // 페이지 이동 시 이미 캡처가 안 되어 있다면 여기서 시도 (JS 리다이렉트 등 대비)
                if (!isBackNavigating && !isSwiping && !isCapturingBeforeNavigation) {
                    captureCurrentState(force = true)
                }
                
                invalidateCaptureState()
                pendingForwardPreviewIndex = resolveForwardPreviewIndex(url)
                awaitingVisualCommit = pendingForwardPreviewIndex != null

                if (isReady && pendingForwardPreviewIndex != null) {
                    freezeCurrentScreen(pendingForwardPreviewIndex)
                }

                if (!isSwiping) isReady = false
            },
            onPageCommitVisibleCallback = { url ->
                updateBackPressState(url)
                handlePageCommitVisible()
            },
            onHistoryUpdatedCallback = { url, isReload ->
                handleVisitedHistoryUpdate(url, isReload)
            },
            onInternalNavigationRequestCallback = { url ->
                navigateWithCapturedPreview(url)
            },
            onPageFinishedCallback = { url ->
                updateBackPressState(url)
                Log.d(
                    WEBVIEW_REFRESH_TAG,
                    "페이지 로드 완료: url=$url"
                )

                if (!hasCompletedLaunchRefresh) {
                    if (!hasIssuedLaunchReload) {
                        if (!isAwaitingLaunchWebCleanup) {
                            isAwaitingLaunchWebCleanup = true
                            clearServiceWorkersAndCacheStorage()
                        }
                        return@AppWebViewClient
                    }

                    hasCompletedLaunchRefresh = true
                    hasIssuedLaunchReload = false
                    isAwaitingLaunchWebCleanup = false
                    logPostRefreshWebDiagnostics()

                    val startupIntent = deferredStartupIntent
                    deferredStartupIntent = null
                    if (startupIntent != null && handleIntent(startupIntent)) {
                        return@AppWebViewClient
                    }
                }
                
                pendingNotificationUrl?.let { notificationUrl ->
                    pendingNotificationUrl = null
                    webView.loadUrl(notificationUrl)
                }

                if (!awaitingVisualCommit && !isBackNavigating) {
                    if (!isReady) isReady = true
                    // 첫 페이지 로드 완료 시 캡처 (이후 SPA 이동 대비)
                    captureCurrentState(force = true)
                }
                currentHistoryIndex = webView.copyBackForwardList().currentIndex
                injectPushStateInterceptor()
            }
        )

        webView.webChromeClient = AppWebChromeClient(this) { callback, _ ->
            handleShowFileChooser(callback)
        }

        webView.addJavascriptInterface(
            WebAppInterface(
                onUpdateRequested = {
                    runOnUiThread { showRefreshDialog() }
                },
                onAppSettingsRequested = {
                    runOnUiThread { openAppSettings() }
                },
                onWebDiagnosticsLogged = { payload ->
                    Log.d(WEBVIEW_REFRESH_TAG, "새로고침 후 웹 진단=$payload")
                },
                onLaunchWebCleanupFinished = { payload ->
                    runOnUiThread {
                        if (isManualRefreshInProgress) handleManualRefreshCleanupFinished(payload)
                        else handleLaunchWebCleanupFinished(payload)
                    }
                },
                onPushState = {
                    runOnUiThread {
                        Log.d("SPA_NAV", "SPA 이동 감지 (pushState) - 즉시 캡처 시도")
                        captureCurrentState(force = true)
                    }
                }
            ),
            "AndroidBridge"
        )

        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            downloadHelper.downloadFile(url, userAgent, contentDisposition, mimeType)
        }
    }

    private fun captureCurrentState(
        force: Boolean = false,
        onComplete: (() -> Unit)? = null
    ) {
        if (webView.width <= 0 || webView.height <= 0 || isSwiping) {
            onComplete?.invoke()
            return
        }
        if (!force && !isReady) {
            onComplete?.invoke()
            return
        }

        val indexToSave = webView.copyBackForwardList().currentIndex
        val currentUrl = webView.url
        if (indexToSave < 0) {
            onComplete?.invoke()
            return
        }
        if (
            !force &&
            indexToSave == lastCapturedIndex &&
            currentUrl == lastCapturedUrl &&
            webView.scrollX == lastCapturedScrollX &&
            webView.scrollY == lastCapturedScrollY
        ) {
            onComplete?.invoke()
            return
        }

        try {
            val width = (webView.width * BITMAP_SCALE).toInt()
            val height = (webView.height * BITMAP_SCALE).toInt()
            if (width <= 0 || height <= 0) {
                onComplete?.invoke()
                return
            }

            val captureRequestId = nextCaptureRequestId++
            latestCaptureRequestIds[indexToSave] = captureRequestId
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            val captureScrollX = webView.scrollX
            val captureScrollY = webView.scrollY
            val location = IntArray(2)
            webView.getLocationInWindow(location)
            val rect = Rect(location[0], location[1], location[0] + webView.width, location[1] + webView.height)

            PixelCopy.request(window, rect, bitmap, { copyResult ->
                val isLatestCapture = latestCaptureRequestIds[indexToSave] == captureRequestId
                if (copyResult == PixelCopy.SUCCESS && isLatestCapture) {
                    lastCapturedIndex = indexToSave
                    lastCapturedUrl = currentUrl
                    lastCapturedScrollX = captureScrollX
                    lastCapturedScrollY = captureScrollY
                    updateBitmapMap(indexToSave, bitmap)
                } else {
                    bitmap.recycle()
                }
                onComplete?.invoke()
            }, Handler(Looper.getMainLooper()))
        } catch (e: Exception) {
            e.printStackTrace()
            onComplete?.invoke()
        }
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

    private fun playForwardAnimation(previewIndex: Int) {
        if (isSwiping) return

        val screenWidth = webView.width.toFloat()
        if (screenWidth <= 0f) {
            webView.translationX = 0f
            fadeOutPreviewImage()
            return
        }

        val prevBitmap = bitmapMap[previewIndex]
        if (prevBitmap != null && !prevBitmap.isRecycled) {
            val interpolator = PathInterpolator(0.22f, 1f, 0.36f, 1f)
            backPreviewImage.animate().cancel()
            webView.animate().cancel()

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
                    .withLayer()
                    .start()
            }
            webView.apply {
                translationX = screenWidth
                elevation = 20f
                animate()
                    .translationX(0f)
                    .setDuration(ANIMATION_DURATION)
                    .setInterpolator(interpolator)
                    .withLayer()
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
    }

    private fun freezeCurrentScreen(previewIndex: Int?) {
        if (isSwiping || previewIndex == null) return

        bitmapMap[previewIndex]?.let {
            if (!it.isRecycled) {
                backPreviewImage.animate().cancel()
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
                pendingForwardPreviewIndex = null
                awaitingVisualCommit = false
                resetBackCrossfadeState()
                backPreviewImage.animate().cancel()
                webView.animate().cancel()
                val currentIndex = webView.copyBackForwardList().currentIndex
                val prevBitmap = bitmapMap[currentIndex - 1]
                if (prevBitmap != null && !prevBitmap.isRecycled) {
                    backPreviewImage.setImageBitmap(prevBitmap)
                    backPreviewImage.visibility = View.VISIBLE
                    backPreviewImage.alpha = 1f
                    backPreviewImage.translationX = 0f
                } else {
                    backPreviewImage.setImageBitmap(null)
                    backPreviewImage.visibility = View.INVISIBLE
                }
                webView.alpha = 1f
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
                isReady = false
                resetBackCrossfadeState()
                val screenWidth = webView.width.toFloat()
                val interpolator = PathInterpolator(0.22f, 1f, 0.36f, 1f)
                val hasBackPreview = backPreviewImage.visibility == View.VISIBLE && backPreviewImage.drawable != null

                webView.animate()
                    .translationX(screenWidth)
                    .setDuration(250)
                    .setInterpolator(interpolator)
                    .withLayer()
                    .withEndAction {
                        isBackAnimationFinished = true
                        webView.translationX = 0f
                        webView.alpha = if (hasBackPreview) 0f else 1f
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) webView.clipToOutline = false
                        tryStartBackCrossfade()
                    }
                    .start()

                backPreviewImage.animate().translationX(0f).alpha(1f).setDuration(250).setInterpolator(interpolator).withLayer().start()

                if (webView.canGoBack()) {
                    webView.goBack()
                    webView.postDelayed({ requestBackTargetVisualReady() }, ANIMATION_DURATION)
                } else {
                    fadeOutPreviewImage()
                }
            }

            override fun handleOnBackCancelled() {
                isSwiping = false
                awaitingVisualCommit = false
                resetBackCrossfadeState()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) webView.clipToOutline = false
                webView.animate().cancel()
                backPreviewImage.animate().cancel()
                webView.animate().translationX(0f).setDuration(300).setInterpolator(PathInterpolator(0.22f, 1f, 0.36f, 1f)).withLayer().start()
                backPreviewImage.animate().alpha(0f).setDuration(250).withLayer().withEndAction { 
                    if (!isSwiping) backPreviewImage.visibility = View.INVISIBLE
                }.start()
            }
        }
        onBackPressedDispatcher.addCallback(this, backPressCallback)
    }

    private fun fadeOutPreviewImage() {
        backPreviewImage.animate().cancel()
        if (backPreviewImage.visibility == View.VISIBLE) {
            val isWebViewFullyCovering = abs(webView.translationX) < 1f && webView.alpha >= 0.99f
            if (isWebViewFullyCovering && !isSwiping) {
                webView.postOnAnimation {
                    if (!isSwiping) {
                        backPreviewImage.visibility = View.INVISIBLE
                        backPreviewImage.alpha = 1f
                        backPreviewImage.setImageBitmap(null)
                    }
                    isBackNavigating = false
                }
                return
            }

            backPreviewImage.animate().alpha(0f).setDuration(180).withLayer().withEndAction {
                if (!isSwiping) {
                    backPreviewImage.visibility = View.INVISIBLE
                    backPreviewImage.alpha = 1f
                    backPreviewImage.setImageBitmap(null)
                }
                isBackNavigating = false
            }.start()
        } else {
            backPreviewImage.alpha = 1f
            backPreviewImage.setImageBitmap(null)
            isBackNavigating = false
        }
    }

    private fun loadInitialPage() {
        if (networkHelper.isInternetAvailable()) webView.loadUrl(Constants.BASE_URL)
        else {
            isReady = true
            showRetryDialog()
        }
    }

    private fun startLaunchRefresh() {
        if (hasCompletedLaunchRefresh || hasIssuedLaunchReload || webView.url != null) {
            Log.d(
                WEBVIEW_REFRESH_TAG,
                "startLaunchRefresh를 건너뜁니다: 완료=$hasCompletedLaunchRefresh, 리로드실행=$hasIssuedLaunchReload, 현재URL=${webView.url}"
            )
            return
        }
        if (!networkHelper.isInternetAvailable()) {
            Log.d(WEBVIEW_REFRESH_TAG, "네트워크를 사용할 수 없어 시작 새로고침을 건너뜁니다")
            isReady = true
            showRetryDialog()
            return
        }

        isReady = false
        Log.d(
            WEBVIEW_REFRESH_TAG,
            "시작 새로고침을 시작합니다: loadUrl(${Constants.BASE_URL})"
        )
        webView.loadUrl(Constants.BASE_URL)
    }

    private fun logWebViewEnvironment() {
        val pkg = WebView.getCurrentWebViewPackage()
        Log.d(
            WEBVIEW_REFRESH_TAG,
            "WebView 패키지=${pkg?.packageName}, 버전명=${pkg?.versionName}, 버전코드=${pkg?.longVersionCode}"
        )
        Log.d(
            WEBVIEW_REFRESH_TAG,
            "초기 userAgent=${webView.settings.userAgentString}"
        )
    }

    private fun clearServiceWorkersAndCacheStorage() {
        val script = """
            (function() {
              const result = {
                href: location.href,
                serviceWorkerSupported: 'serviceWorker' in navigator,
                cacheStorageSupported: 'caches' in window
              };
              const tasks = [];
              try {
                if ('serviceWorker' in navigator) {
                  tasks.push(
                    navigator.serviceWorker.getRegistrations()
                      .then(function(regs) {
                        result.registrationCount = regs.length;
                        return Promise.allSettled(
                          regs.map(function(reg) {
                            return reg.unregister().then(function(unregistered) {
                              return {
                                scope: reg.scope,
                                unregistered: unregistered
                              };
                            });
                          })
                        ).then(function(items) {
                          result.serviceWorkerUnregisterResults = items.map(function(item) {
                            return item.status === 'fulfilled' ? item.value : String(item.reason);
                          });
                        });
                      })
                      .catch(function(e) {
                        result.serviceWorkerCleanupError = String(e);
                      })
                  );
                }
              } catch (e) {
                result.serviceWorkerCleanupError = String(e);
              }
              try {
                if ('caches' in window) {
                  tasks.push(
                    caches.keys()
                      .then(function(names) {
                        result.cacheNamesBeforeDelete = names;
                        return Promise.allSettled(
                          names.map(function(name) {
                            return caches.delete(name).then(function(deleted) {
                              return {
                                name: name,
                                deleted: deleted
                              };
                            });
                          })
                        ).then(function(items) {
                          result.cacheDeleteResults = items.map(function(item) {
                            return item.status === 'fulfilled' ? item.value : String(item.reason);
                          });
                        });
                      })
                      .catch(function(e) {
                        result.cacheCleanupError = String(e);
                      })
                  );
                }
              } catch (e) {
                result.cacheCleanupError = String(e);
              }
              Promise.allSettled(tasks).then(function() {
                const payload = JSON.stringify(result);
                if (window.AndroidBridge && window.AndroidBridge.onLaunchWebCleanupFinished) {
                  window.AndroidBridge.onLaunchWebCleanupFinished(payload);
                } else {
                  console.log("시작 웹 정리 완료=" + payload);
                }
              });
              return "scheduled";
            })();
        """.trimIndent()

        webView.evaluateJavascript(script) { result ->
            Log.d(WEBVIEW_REFRESH_TAG, "시작 웹 정리 스크립트 실행 결과=$result")
        }
    }

    private fun handleLaunchWebCleanupFinished(payload: String) {
        Log.d(WEBVIEW_REFRESH_TAG, "시작 웹 정리 완료 결과=$payload")

        if (hasCompletedLaunchRefresh || hasIssuedLaunchReload) return

        isAwaitingLaunchWebCleanup = false
        hasIssuedLaunchReload = true
        Log.d(WEBVIEW_REFRESH_TAG, "시작 웹 정리가 끝나 webView.reload()를 실행합니다")
        webView.reload()
    }

    private fun handleManualRefreshCleanupFinished(payload: String) {
        Log.d(WEBVIEW_REFRESH_TAG, "수동 새로고침 정리 완료 결과=$payload")

        if (!isManualRefreshInProgress) return

        isManualRefreshInProgress = false
        val targetUrl = webView.url ?: Constants.BASE_URL
        Log.d(WEBVIEW_REFRESH_TAG, "수동 새로고침 정리가 끝나 URL을 다시 로드합니다: $targetUrl")
        webView.loadUrl(targetUrl)
    }

    private fun logPostRefreshWebDiagnostics() {
        if (hasLoggedPostRefreshWebDiagnostics) return
        if (!webView.url.orEmpty().startsWith(Constants.BASE_URL)) return

        hasLoggedPostRefreshWebDiagnostics = true
        val script = """
            (function() {
              const details = {
                href: location.href,
                userAgent: navigator.userAgent,
                serviceWorkerSupported: 'serviceWorker' in navigator,
                serviceWorkerControlled: !!(navigator.serviceWorker && navigator.serviceWorker.controller),
                serviceWorkerController: navigator.serviceWorker && navigator.serviceWorker.controller ? navigator.serviceWorker.controller.scriptURL : null,
                cacheStorageSupported: 'caches' in window
              };
              try {
                details.localStorageLength = localStorage.length;
              } catch (e) {
                details.localStorageLength = "error:" + e.message;
              }
              try {
                details.sessionStorageLength = sessionStorage.length;
              } catch (e) {
                details.sessionStorageLength = "error:" + e.message;
              }
              const tasks = [];
              try {
                if ('serviceWorker' in navigator) {
                  tasks.push(
                    navigator.serviceWorker.getRegistrations()
                      .then(function(regs) {
                        details.serviceWorkerRegistrations = regs.map(function(reg) {
                          return {
                            scope: reg.scope,
                            active: reg.active ? reg.active.scriptURL : null,
                            waiting: reg.waiting ? reg.waiting.scriptURL : null,
                            installing: reg.installing ? reg.installing.scriptURL : null
                          };
                        });
                      })
                      .catch(function(e) {
                        details.serviceWorkerRegistrationsError = String(e);
                      })
                  );
                }
              } catch (e) {
                details.serviceWorkerRegistrationsError = String(e);
              }
              try {
                if ('caches' in window) {
                  tasks.push(
                    caches.keys()
                      .then(function(names) {
                        details.cacheNames = names;
                      })
                      .catch(function(e) {
                        details.cacheNamesError = String(e);
                      })
                  );
                }
              } catch (e) {
                details.cacheNamesError = String(e);
              }
              try {
                tasks.push(
                  fetch(location.href, { cache: 'no-store', credentials: 'include' })
                    .then(function(response) {
                      details.noStoreFetch = {
                        ok: response.ok,
                        status: response.status,
                        cacheControl: response.headers.get('cache-control'),
                        etag: response.headers.get('etag'),
                        lastModified: response.headers.get('last-modified')
                      };
                    })
                    .catch(function(e) {
                      details.noStoreFetchError = String(e);
                    })
                );
              } catch (e) {
                details.noStoreFetchError = String(e);
              }
              Promise.allSettled(tasks).then(function() {
                const payload = JSON.stringify(details);
                if (window.AndroidBridge && window.AndroidBridge.logWebDiagnostics) {
                  window.AndroidBridge.logWebDiagnostics(payload);
                } else {
                  console.log("새로고침 후 웹 진단=" + payload);
                }
              });
              return "scheduled";
            })();
        """.trimIndent()

        webView.evaluateJavascript(script) { result ->
            Log.d(WEBVIEW_REFRESH_TAG, "새로고침 후 웹 진단 스크립트 실행 결과=$result")
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

    private fun resolveForwardPreviewIndex(targetUrl: String?): Int? {
        if (isSwiping || isBackNavigating) return null

        val targetPath = targetUrl?.replace(Constants.BASE_URL, "") ?: ""
        if (targetPath.isEmpty() || targetPath in Constants.RESTRICTED_PATHS) return null

        val list = webView.copyBackForwardList()
        val currentIndex = list.currentIndex
        val currentUrl = if (currentIndex >= 0) list.getItemAtIndex(currentIndex)?.url else webView.url
        if (!currentUrl.isNullOrBlank() && currentUrl == targetUrl) return null

        return if (currentIndex >= 0) currentIndex else currentHistoryIndex.takeIf { it >= 0 }
    }

    private fun navigateWithCapturedPreview(url: String): Boolean {
        if (url.isBlank()) return false
        if (isSwiping || isBackNavigating) return false
        if (url == webView.url) return false

        pendingCapturedNavigationUrl = url
        if (isCapturingBeforeNavigation) {
            return true
        }

        isCapturingBeforeNavigation = true
        captureCurrentState(force = true) {
            val nextUrl = pendingCapturedNavigationUrl
            pendingCapturedNavigationUrl = null
            isCapturingBeforeNavigation = false

            if (!nextUrl.isNullOrBlank()) {
                webView.post { webView.loadUrl(nextUrl) }
            }
        }
        return true
    }

    private fun runAfterVisualState(onReady: () -> Unit) {
        if (webView.width <= 0 || webView.height <= 0) {
            webView.post { onReady() }
            return
        }

        val requestId = nextVisualStateRequestId++
        try {
            webView.postVisualStateCallback(requestId, object : WebView.VisualStateCallback() {
                override fun onComplete(requestId: Long) {
                    webView.post { onReady() }
                }
            })
        } catch (_: Exception) {
            webView.post { onReady() }
        }
    }

    private fun requestBackTargetVisualReady() {
        if (!isBackNavigating || isBackTargetVisualReady || isAwaitingBackVisualState) return

        isAwaitingBackVisualState = true
        runAfterVisualState {
            isAwaitingBackVisualState = false
            if (!isBackNavigating) return@runAfterVisualState

            isBackTargetVisualReady = true
            tryStartBackCrossfade()
        }
    }

    private fun tryStartBackCrossfade() {
        if (!isBackNavigating || !isBackAnimationFinished || !isBackTargetVisualReady) return

        if (backPreviewImage.visibility != View.VISIBLE || backPreviewImage.drawable == null) {
            webView.alpha = 1f
            isReady = true
            resetBackCrossfadeState()
            isBackNavigating = false
            return
        }

        val webViewFadeInInterpolator = PathInterpolator(0.22f, 1f, 0.36f, 1f)
        val previewFadeOutInterpolator = PathInterpolator(0.3f, 0f, 0.8f, 1f)
        webView.animate().cancel()
        backPreviewImage.animate().cancel()
        webView.translationX = 0f
        webView.visibility = View.VISIBLE
        webView.alpha = BACK_CROSSFADE_WEBVIEW_START_ALPHA
        backPreviewImage.visibility = View.VISIBLE
        backPreviewImage.alpha = 1f
        backPreviewImage.translationX = 0f

        webView.animate()
            .alpha(1f)
            .setDuration(BACK_CROSSFADE_DURATION)
            .setInterpolator(webViewFadeInInterpolator)
            .withLayer()
            .start()

        backPreviewImage.animate()
            .alpha(0f)
            .setStartDelay(BACK_CROSSFADE_PREVIEW_DELAY)
            .setDuration(BACK_CROSSFADE_DURATION + 20L)
            .setInterpolator(previewFadeOutInterpolator)
            .withLayer()
            .withEndAction {
                backPreviewImage.visibility = View.INVISIBLE
                backPreviewImage.alpha = 1f
                backPreviewImage.setImageBitmap(null)
                isReady = true
                resetBackCrossfadeState()
                isBackNavigating = false
            }
            .start()
    }

    private fun resetBackCrossfadeState() {
        isBackAnimationFinished = false
        isBackTargetVisualReady = false
        isAwaitingBackVisualState = false
    }

    private fun finalizeForwardTransition(previewIndex: Int, expectedHistoryIndex: Int) {
        runAfterVisualState {
            val currentIndex = webView.copyBackForwardList().currentIndex
            val shouldAnimate = !isBackNavigating &&
                !isSwiping &&
                awaitingVisualCommit &&
                pendingForwardPreviewIndex == previewIndex &&
                currentIndex == expectedHistoryIndex

            if (shouldAnimate) {
                playForwardAnimation(previewIndex)
                currentHistoryIndex = currentIndex
            } else {
                webView.translationX = 0f
                fadeOutPreviewImage()
            }

            awaitingVisualCommit = false
            pendingForwardPreviewIndex = null
            isReady = true
        }
    }

    private fun handleVisitedHistoryUpdate(url: String?, isReload: Boolean) {
        updateBackPressState(url)

        val previousIndex = currentHistoryIndex
        val previousUrl = lastCapturedUrl
        val currentIndex = webView.copyBackForwardList().currentIndex
        currentHistoryIndex = currentIndex

        if (!hasCompletedLaunchRefresh) {
            return
        }

        if (!url.isNullOrBlank() && url != previousUrl) {
            invalidateCaptureState()
        }

        if (isBackNavigating) {
            awaitingVisualCommit = false
            pendingForwardPreviewIndex = null
            webView.visibility = View.VISIBLE
            requestBackTargetVisualReady()
            return
        }

        if (!awaitingVisualCommit && !isReload && !isSwiping && previousIndex >= 0 && currentIndex > previousIndex) {
            pendingForwardPreviewIndex = previousIndex
            awaitingVisualCommit = true
            isReady = false
            freezeCurrentScreen(previousIndex)
            finalizeForwardTransition(previousIndex, currentIndex)
        } else if (!isReload) {
            runAfterVisualState {
                isReady = true
            }
        }
    }

    private fun handlePageCommitVisible() {
        val committedIndex = webView.copyBackForwardList().currentIndex
        val previewIndex = pendingForwardPreviewIndex

        if (!hasCompletedLaunchRefresh) {
            currentHistoryIndex = committedIndex
            return
        }

        if (isBackNavigating) {
            webView.visibility = View.VISIBLE
            currentHistoryIndex = committedIndex
            requestBackTargetVisualReady()
            return
        }

        if (awaitingVisualCommit && previewIndex != null) {
            playForwardAnimation(previewIndex)
        } else {
            webView.translationX = 0f
            fadeOutPreviewImage()
        }

        awaitingVisualCommit = false
        pendingForwardPreviewIndex = null
        currentHistoryIndex = committedIndex
        isReady = true
    }

    private fun invalidateCaptureState() {
        lastCapturedIndex = -1
        lastCapturedUrl = null
        lastCapturedScrollX = Int.MIN_VALUE
        lastCapturedScrollY = Int.MIN_VALUE
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
        AlertDialog.Builder(this).setTitle("인터넷 연결 없음").setMessage("연결 후 다시 시도해주세요.").setPositiveButton("재시도") { _, _ ->
            if (hasCompletedLaunchRefresh) loadInitialPage()
            else startLaunchRefresh()
        }.setNegativeButton("종료") { _, _ -> finish() }.setCancelable(false).show()
    }

    private fun showRefreshDialog() {
        AlertDialog.Builder(this).setTitle("업데이트").setMessage("최신 버전으로 업데이트할까요?").setPositiveButton("확인") { _, _ ->
            refreshWebView()
        }.setNegativeButton("취소", null).show()
    }

    private fun refreshWebView() {
        if (isManualRefreshInProgress) {
            Log.d(WEBVIEW_REFRESH_TAG, "수동 새로고침이 이미 진행 중입니다")
            return
        }

        isManualRefreshInProgress = true
        Log.d(WEBVIEW_REFRESH_TAG, "수동 새로고침을 시작합니다: clearCache(true) + 서비스워커 정리")
        webView.clearCache(true)
        clearServiceWorkersAndCacheStorage()
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        runCatching {
            startActivity(intent)
        }.onFailure {
            showToast("설정 화면을 열 수 없습니다.")
        }
    }

    private fun injectPushStateInterceptor() {
        val script = """
            (function() {
                if (window._pushStateIntercepted) return;
                window._pushStateIntercepted = true;
                
                const originalPushState = history.pushState;
                const originalReplaceState = history.replaceState;
                
                history.pushState = function() {
                    const args = arguments;
                    const self = this;
                    if (window.AndroidBridge && window.AndroidBridge.onPushState) {
                        window.AndroidBridge.onPushState();
                        // 앱이 캡처를 시작할 수 있도록 10ms 정도의 찰나의 지연을 줌
                        setTimeout(function() {
                            originalPushState.apply(self, args);
                        }, 10);
                    } else {
                        originalPushState.apply(self, args);
                    }
                };
                
                history.replaceState = function() {
                    const args = arguments;
                    const self = this;
                    if (window.AndroidBridge && window.AndroidBridge.onPushState) {
                        window.AndroidBridge.onPushState();
                        setTimeout(function() {
                            originalReplaceState.apply(self, args);
                        }, 10);
                    } else {
                        originalReplaceState.apply(self, args);
                    }
                };
            })();
        """.trimIndent()
        webView.evaluateJavascript(script, null)
    }

    override fun onDestroy() {
        super.onDestroy()
        FcmTokenBridge.detachWebView(webView)
        bitmapMap.values.forEach { if (!it.isRecycled) it.recycle() }
        bitmapMap.clear()
    }
}
