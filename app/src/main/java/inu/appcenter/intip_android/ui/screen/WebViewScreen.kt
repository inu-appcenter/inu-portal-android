package inu.appcenter.intip_android.ui.screen

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
    authViewModel: AuthViewModel
) {
    val uiState by authViewModel.uiState.collectAsState()

    LaunchedEffect(uiState.hasToken, uiState.token) {
        Log.d("WebViewScreen", "유효 토큰 여부: ${uiState.hasToken}, Token: ${uiState.token}")
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
        when {
            uiState.hasToken == true && !uiState.token.isNullOrEmpty() -> {
                CustomAndroidView(
                    modifier = Modifier.padding(newPaddingValue),
                    path = path,
                    token = uiState.token,   // 토큰 전달
                    navController = navController
                )
            }
            uiState.hasToken == true && uiState.token.isNullOrEmpty() -> {
                // 토큰 로딩 중 또는 에러 상태 처리
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(newPaddingValue),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            else -> {
                // 로그인 화면 또는 에러 메시지 표시
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(newPaddingValue),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "토큰이 유효하지 않습니다. 다시 로그인해주세요.")
                }
            }
        }
    }
}