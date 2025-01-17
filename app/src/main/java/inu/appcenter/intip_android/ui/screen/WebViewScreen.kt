@file:Suppress("DEPRECATION")

package inu.appcenter.intip_android.ui.screen

import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import inu.appcenter.intip_android.ui.component.AppBottomBar
import inu.appcenter.intip_android.ui.component.CustomAndroidView

@Composable
fun WebViewScreen(modifier: Modifier = Modifier, navController: NavHostController, path: String) {
    //todo: 나중에 전역 변수나 env로 빼기
    val WEB_BASE_URL = "https://intip.inuappcenter.kr/app"

    Scaffold(
        bottomBar = {
            AppBottomBar(navController)
        },
        containerColor = Color.Red
    ) { paddingValue ->
        val newPaddingValue = PaddingValues(
            start = paddingValue.calculateStartPadding(LayoutDirection.Ltr),
            end = paddingValue.calculateEndPadding(LayoutDirection.Rtl),
            top = 0.dp,
            bottom = paddingValue.calculateBottomPadding() - 20.dp,
            // Scafflod의 기본 BottomPadding에서 BottomNavigation의 둥근 정도를 뺀 값
        )
        CustomAndroidView(
            modifier = Modifier.padding(newPaddingValue),
            path = path
        )

    }
}