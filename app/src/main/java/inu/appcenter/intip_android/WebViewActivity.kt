package inu.appcenter.intip_android

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.ValueCallback
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

class WebViewActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        handleFileSelection(uri?.let { listOf(it) })
    }

    private val pickMultipleImagesLauncher = registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(5)) { uris ->
        handleFileSelection(uris)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)

        val url = intent.getStringExtra("URL") ?: Constants.BASE_URL

        val container = android.widget.FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            fitsSystemWindows = true
            setBackgroundColor(getColor(R.color.app_background_color))
        }

        webView = WebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(getColor(R.color.app_background_color))
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
        }

        val progressBar = android.widget.ProgressBar(this).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.CENTER
            }
            scaleX = 0.60f //로딩스피너 크기
            scaleY = 0.60f
            indeterminateTintList = android.content.res.ColorStateList.valueOf(getColor(R.color.intip_color))
        }

        container.addView(webView)
        container.addView(progressBar)
        setContentView(container)

        setupWebView(progressBar)

        webView.loadUrl(url)
    }

    override fun onBackPressed() {
        requestBackWithUnsavedChangesCheck()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(progressBar: android.widget.ProgressBar) {
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
            onPageCommitVisibleCallback = {
                runOnUiThread {
                    progressBar.visibility = View.GONE
                }
            },
            onPageFinishedCallback = { url ->
                Log.d("WebViewActivity", "Page finished loading: $url")
                runOnUiThread {
                    progressBar.visibility = View.GONE
                }
            }
        )

        webView.webChromeClient = AppWebChromeClient(this) { callback, _ ->
            handleShowFileChooser(callback)
        }

        webView.addJavascriptInterface(
            WebAppInterface(
                onUpdateRequested = {},
                onAppSettingsRequested = { openAppSettings() },
                onNavigateToRequested = { destination, subUrl ->
                    runOnUiThread {
                        val intent = Intent(this, WebViewActivity::class.java).apply {
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
                        requestBackWithUnsavedChangesCheck()
                    }
                },
                onBackConfirmed = {
                    runOnUiThread {
                        handleBackRequest()
                    }
                }
            ),
            "AndroidBridge"
        )
    }

    override fun finish() {
        super.finish()
        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, R.anim.slide_in_left, R.anim.slide_out_right)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
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

    private fun requestBackWithUnsavedChangesCheck() {
        val script = """
            (function() {
              if (window.__intipHasUnsavedChanges && typeof window.__intipHandleNativeBackRequest === 'function') {
                return window.__intipHandleNativeBackRequest() ? 'blocked' : 'allow';
              }
              return 'allow';
            })();
        """.trimIndent()

        webView.evaluateJavascript(script) { result ->
            if (result == "\"blocked\"") {
                return@evaluateJavascript
            }
            runOnUiThread {
                handleBackRequest()
            }
        }
    }

    private fun handleBackRequest() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            finish()
        }
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
        webView.destroy()
    }
}
