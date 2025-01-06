package inu.appcenter.intip_android

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import inu.appcenter.intip_android.ui.component.BottomNav
import inu.appcenter.intip_android.ui.theme.INTIPTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            INTIPTheme {
                Scaffold(
                    bottomBar = {
                        BottomNav()
                    },
                ) { paddingValue ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                start = paddingValue.calculateStartPadding(LayoutDirection.Ltr),
                                end = paddingValue.calculateEndPadding(LayoutDirection.Rtl),
                                top = paddingValue.calculateTopPadding(),
                                bottom = 0.dp
                            )
                    ) {
                        WebViewScreen()
                    }
                }
            }
        }
    }
}

@Composable
fun WebViewScreen(
    modifier: Modifier = Modifier
) {
    Icons.Default.Add
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true // 필요하다면 JS 활성화
                settings.domStorageEnabled = true // 필요하다면 DOM 저장소 활성화
                webViewClient = WebViewClient()   // 외부 브라우저로 안 나가게
                loadUrl("https://intip.inuappcenter.kr/app/home")
            }
        }
    )
}