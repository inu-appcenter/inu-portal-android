package inu.appcenter.intip_android.ui.component

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun WebViewScreen(
    modifier: Modifier = Modifier,
    url : String
) {
    Icons.Default.Add
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true // 필요하다면 JS 활성화
                settings.domStorageEnabled = true // 필요하다면 DOM 저장소 활성화
                webViewClient = WebViewClient()   // 외부 브라우저로 안 나가게
                loadUrl(url)
            }
        }
    )
}