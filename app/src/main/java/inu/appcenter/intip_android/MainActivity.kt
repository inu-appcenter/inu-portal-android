package inu.appcenter.intip_android

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.webkit.ValueCallback
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    
    private var hasCompletedLaunchRefresh = false
    private var hasIssuedLaunchReload = false
    private var isAwaitingLaunchWebCleanup = false
    private var isManualRefreshInProgress = false
    private var deferredStartupIntent: Intent? = null
    private var pendingNotificationUrl: String? = null

    private val networkHelper by lazy { NetworkHelper(this) }
    private val downloadHelper by lazy { DownloadHelper(this) }
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

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

    private val mainHandler by lazy { android.os.Handler(android.os.Looper.getMainLooper()) }
    private val splashTimeoutRunnable = Runnable {
        if (!hasCompletedLaunchRefresh) {
            Log.w("MainActivity", "스플래시 화면 타임아웃 도달 (5초) - 강제로 launch refresh 완료 처리")
            hasCompletedLaunchRefresh = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContentView(R.layout.activity_main)

        splashScreen.setKeepOnScreenCondition { !hasCompletedLaunchRefresh }
        mainHandler.postDelayed(splashTimeoutRunnable, 5000)

        createNotificationChannel()
        setupViews()
        setupWebView()
        logWebViewEnvironment()
        checkAndRequestPermissions()

        startLaunchRefresh()
        handleIntent(intent)
        logFcmToken()
    }

    override fun onResume() {
        super.onResume()
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
        availability.makeGooglePlayServicesAvailable(this).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                fetchAndLogFcmToken()
            } else {
                val resultCode = availability.isGooglePlayServicesAvailable(this)
                Log.w("FCM_TOKEN", "Google Play Services를 사용할 수 없습니다. 코드: $resultCode")
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
            deferredStartupIntent = intent
            return false
        }

        val targetPath = intent.getStringExtra("TARGET_PATH")
        val isFromNotification = intent.hasExtra("google.message_id") || intent.hasExtra("google.sent_time") || targetPath != null

        if (isFromNotification && targetPath != null) {
            val url = if (targetPath.startsWith("http")) targetPath 
                      else Constants.BASE_URL + if (targetPath.startsWith("/")) targetPath else "/$targetPath"
            
            if (webView.url != null) {
                webView.loadUrl(url)
                return true
            } else {
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
            onPageFinishedCallback = { url ->
                Log.d("MainActivity", "페이지 로드 완료: url=$url")

                if (!hasCompletedLaunchRefresh) {
                    hasCompletedLaunchRefresh = true
                    mainHandler.removeCallbacks(splashTimeoutRunnable)
                }
                
                pendingNotificationUrl?.let { notificationUrl ->
                    pendingNotificationUrl = null
                    webView.loadUrl(notificationUrl)
                }

                val startupIntent = deferredStartupIntent
                deferredStartupIntent = null
                if (startupIntent != null) {
                    handleIntent(startupIntent)
                }
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
                    Log.d("MainActivity", "새로고침 후 웹 진단=$payload")
                },
                onLaunchWebCleanupFinished = { payload ->
                    runOnUiThread {
                        if (isManualRefreshInProgress) handleManualRefreshCleanupFinished(payload)
                        else handleLaunchWebCleanupFinished(payload)
                    }
                },
                onNavigateToRequested = { destination, subUrl ->
                    runOnUiThread {
                        val intent = Intent(this@MainActivity, WebViewActivity::class.java).apply {
                            putExtra("URL", subUrl)
                            putExtra("DESTINATION", destination)
                        }
                        startActivity(intent)
                        if (Build.VERSION.SDK_INT >= 34) {
                            overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, R.anim.slide_in_right, R.anim.slide_out_left)
                        } else {
                            @Suppress("DEPRECATION")
                            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                        }
                    }
                },
                onGoBackRequested = {
                    runOnUiThread {
                        finish()
                    }
                }
            ),
            "AndroidBridge"
        )

        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            downloadHelper.downloadFile(url, userAgent, contentDisposition, mimeType)
        }
    }

    private fun loadInitialPage() {
        if (networkHelper.isInternetAvailable()) webView.loadUrl(Constants.BASE_URL)
        else {
            hasCompletedLaunchRefresh = true
            showRetryDialog()
        }
    }

    private fun startLaunchRefresh() {
        if (hasCompletedLaunchRefresh || hasIssuedLaunchReload || webView.url != null) {
            return
        }
        if (!networkHelper.isInternetAvailable()) {
            hasCompletedLaunchRefresh = true
            showRetryDialog()
            return
        }
        webView.loadUrl(Constants.BASE_URL)
    }

    private fun logWebViewEnvironment() {
        val pkg = WebView.getCurrentWebViewPackage()
        Log.d("MainActivity", "WebView 패키지=${pkg?.packageName}, 버전명=${pkg?.versionName}")
    }

    private fun clearServiceWorkersAndCacheStorage() {
        val script = """
            (function() {
              var bridge = window.AndroidBridge;
              function notify() {
                if (bridge && typeof bridge.onLaunchWebCleanupFinished === 'function') {
                  try { bridge.onLaunchWebCleanupFinished("done"); } catch(e) {}
                }
              }
              try {
                var p1 = ('serviceWorker' in navigator) ? navigator.serviceWorker.getRegistrations().catch(function(){return [];}) : Promise.resolve([]);
                var p2 = ('caches' in window) ? caches.keys().catch(function(){return [];}) : Promise.resolve([]);
                Promise.all([p1, p2]).then(function(results) {
                  var regs = results[0] || [];
                  var keys = results[1] || [];
                  var unreg = regs.map(function(r) { return r.unregister().catch(function(){}); });
                  var del = keys.map(function(k) { return caches.delete(k).catch(function(){}); });
                  return Promise.allSettled(unreg.concat(del));
                }).catch(function(){}).finally(function() {
                  notify();
                });
              } catch(e) {
                notify();
              }
            })();
        """.trimIndent()

        webView.evaluateJavascript(script) { result ->
            Log.d("MainActivity", "ServiceWorker cleanup script scheduled: $result")
        }
    }

    private fun handleLaunchWebCleanupFinished(payload: String) {
        if (hasCompletedLaunchRefresh || hasIssuedLaunchReload) return
        isAwaitingLaunchWebCleanup = false
        hasIssuedLaunchReload = true
        webView.reload()
    }

    private fun handleManualRefreshCleanupFinished(payload: String) {
        if (!isManualRefreshInProgress) return
        isManualRefreshInProgress = false
        webView.loadUrl(webView.url ?: Constants.BASE_URL)
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

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        
        permissionLauncher.launch(permissions.toTypedArray())
    }

    private fun showToast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    private fun showRetryDialog() {
        AlertDialog.Builder(this)
            .setTitle("인터넷 연결 없음")
            .setMessage("연결 후 다시 시도해주세요.")
            .setPositiveButton("재시도") { _, _ ->
                if (hasCompletedLaunchRefresh) loadInitialPage()
                else startLaunchRefresh()
            }
            .setNegativeButton("종료") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    private fun showRefreshDialog() {
        AlertDialog.Builder(this)
            .setTitle("업데이트")
            .setMessage("최신 버전으로 업데이트할까요?")
            .setPositiveButton("확인") { _, _ ->
                refreshWebView()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun refreshWebView() {
        if (isManualRefreshInProgress) return
        isManualRefreshInProgress = true
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
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mainHandler.removeCallbacksAndMessages(null)
        FcmTokenBridge.detachWebView(webView)
        webView.destroy()
    }
}
