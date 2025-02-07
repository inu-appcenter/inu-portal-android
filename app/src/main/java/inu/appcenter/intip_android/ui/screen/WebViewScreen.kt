package inu.appcenter.intip_android.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
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
import inu.appcenter.intip_android.utils.K

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
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    // WindowInsets.statusBars.asPaddingValues()로 상태바의 패딩 값을 구한 후, calculateTopPadding()으로 높이를 가져옴
                    // 4dp 추가한 이유는 상단의 공백과 동일하게 하단의 공백을 맞추기 위해서 추가함
                    .height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 4.dp)
                    .background(Color.White)
            )
        },
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
                top = paddingValue.calculateTopPadding(),
                // Scafflod의 기본 BottomPadding에서 BottomNavigation의 둥근 정도를 뺀 값
                bottom = paddingValue.calculateBottomPadding() - K.BOTTOM_BAR_OFFSET
            )
        } else {
            PaddingValues(
                start = paddingValue.calculateStartPadding(LayoutDirection.Ltr),
                end = paddingValue.calculateEndPadding(LayoutDirection.Rtl),
                top = paddingValue.calculateTopPadding(),
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