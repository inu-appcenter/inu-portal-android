package inu.appcenter.intip_android

import android.content.pm.PackageManager
import android.os.Build
import android.Manifest
import android.os.Bundle
import android.util.Log
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.firebase.messaging.FirebaseMessaging
import inu.appcenter.intip_android.ui.login.AuthViewModel
import inu.appcenter.intip_android.ui.navigate.MyApp
import inu.appcenter.intip_android.ui.theme.INTIPTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    private lateinit var webView: WebView
    private val authViewModel : AuthViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.web_main)

        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = true
            displayZoomControls = false
        }

        webView.webViewClient = object : WebViewClient() {}
        webView.webChromeClient = WebChromeClient()
        webView.loadUrl("https://intip.inuappcenter.kr/m/home")

        onBackPressedDispatcher.addCallback(this) {
            if (webView.canGoBack()) webView.goBack() else finish()
        }
    }

    override fun onStart() {
        super.onStart()

        requestNotificationPermission()
        authViewModel.getFCMToken()
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
    }
    override fun onResume() {
        super.onResume()
        webView.onResume()
    }
    override fun onDestroy() {
        webView.apply {
            loadUrl("about:blank")
            stopLoading()
            webChromeClient = null
            destroy()
        }
        super.onDestroy()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13 이상
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }
    }
}

