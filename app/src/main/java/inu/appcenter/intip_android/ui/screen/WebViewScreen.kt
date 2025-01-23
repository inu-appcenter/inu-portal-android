@file:Suppress("DEPRECATION")

package inu.appcenter.intip_android.ui.screen

import android.util.Log
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import inu.appcenter.intip_android.ui.component.AppBottomBar
import inu.appcenter.intip_android.ui.component.CustomAndroidView
import inu.appcenter.intip_android.ui.login.AuthViewModel

@Composable
fun WebViewScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    path: String,
    authViewModel: AuthViewModel  // 추가
) {
    // 상위에서 tokenFlow를 구독하여 토큰을 가져옵니다.
    val token by authViewModel.tokenFlow.collectAsState(initial = null)

    LaunchedEffect(token) {
        Log.d("WebViewScreen", "최신 토큰 값: $token")
    }

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
            bottom = paddingValue.calculateBottomPadding() - 20.dp // Scafflod의 기본 BottomPadding에서 BottomNavigation의 둥근 정도를 뺀 값
        )
        CustomAndroidView(
            modifier = Modifier.padding(newPaddingValue),
            path = path,
            token = token   // 토큰 전달
        )
    }
}