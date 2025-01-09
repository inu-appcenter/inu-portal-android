package inu.appcenter.intip_android.ui.web

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import inu.appcenter.intip_android.ui.component.AppBottomBar

@Composable
fun WebViewScreen(modifier: Modifier = Modifier, navController: NavHostController, path: String) {
    //todo: 나중에 전역 변수나 env로 빼기
    val WEB_BASE_URL = "https://intip.inuappcenter.kr/app"

    Scaffold(
        bottomBar = {
            AppBottomBar(navController)
        }
    ) {
        AndroidView(
            modifier = Modifier.padding(it),
            factory = { context ->
                WebView(context).apply {
                    // 디버깅 활성화
                    WebView.setWebContentsDebuggingEnabled(true)

                    // WebView 설정
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    settings.setSupportZoom(true)
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false

                    webViewClient = WebViewClient()
                    loadUrl("${WEB_BASE_URL}${path}")
                }
            })
    }
}