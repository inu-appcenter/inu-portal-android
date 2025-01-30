package inu.appcenter.intip_android.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import inu.appcenter.intip_android.ui.component.AppBottomBar
import inu.appcenter.intip_android.ui.component.CustomAndroidView
import inu.appcenter.intip_android.ui.login.AuthViewModel
import inu.appcenter.intip_android.ui.navigate.AllDestination

@Composable
fun WebViewScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    path: String,
    authViewModel: AuthViewModel,
    isShowBottomBar: Boolean
) {
    val uiState by authViewModel.uiState.collectAsState()

    Scaffold(
        bottomBar = {
            if (isShowBottomBar) {
                AppBottomBar(navController)
            }
        }
    ) { paddingValue ->
        val newPaddingValue = if (isShowBottomBar) {
            PaddingValues(
                start = paddingValue.calculateStartPadding(LayoutDirection.Ltr),
                end = paddingValue.calculateEndPadding(LayoutDirection.Rtl),
                top = 0.dp,
                bottom = paddingValue.calculateBottomPadding() - 20.dp // Scafflod의 기본 BottomPadding에서 BottomNavigation의 둥근 정도를 뺀 값
            )
        } else {
            PaddingValues(
                start = paddingValue.calculateStartPadding(LayoutDirection.Ltr),
                end = paddingValue.calculateEndPadding(LayoutDirection.Rtl),
                top = 0.dp,  // 상태바 영역의 패딩을 제거
                bottom = paddingValue.calculateBottomPadding()
            )
        }
        // 유효 토큰이 있는 경우에만 WebView 보여주기
        CustomAndroidView(
            modifier = Modifier.padding(newPaddingValue),
            path = path,
            token = uiState.token,   // 토큰 전달
            navController = navController,
            authViewModel = authViewModel
        )

    }
}