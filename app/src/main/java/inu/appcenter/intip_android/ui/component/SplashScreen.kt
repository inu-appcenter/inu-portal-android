package inu.appcenter.intip_android.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import inu.appcenter.intip_android.R
import inu.appcenter.intip_android.ui.navigate.AllDestination

@Composable
fun SplashScreen(navController: NavController) {
    // 스플래시 화면에서 일정 시간 대기 후 홈 화면으로 네비게이션
    LaunchedEffect(key1 = true) {
        delay(2000) // 2초 대기 (필요에 따라 조정)
        navController.navigate(AllDestination.Home.route) {
            popUpTo(AllDestination.Splash.route) { inclusive = true }
        }
    }

    // 스플래시 화면 UI
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // 로고 이미지 예시 (res/drawable/ic_splash_logo.png 파일이 필요)
        Image(
            painter = painterResource(id = R.drawable.icon_profile),
            contentDescription = "Splash Logo"
        )
    }
}
