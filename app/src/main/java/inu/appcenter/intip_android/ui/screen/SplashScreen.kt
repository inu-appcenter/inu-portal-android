package inu.appcenter.intip_android.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import inu.appcenter.intip_android.ui.login.AuthViewModel
import inu.appcenter.intip_android.ui.navigate.AllDestination
import inu.appcenter.intip_android.utils.K
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController, authViewModel: AuthViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current

    // SVG 디코딩을 위한 ImageLoader 구성
    val imageLoader = ImageLoader.Builder(context)
        .components { add(SvgDecoder.Factory()) }
        .build()

    // 스플래시 화면에서 일정 시간 대기 전에 토큰 유효성 확인 및 갱신
    LaunchedEffect(key1 = true) {
        // 토큰이 만료되었으면 refreshToken()을 호출해서 갱신
        authViewModel.ensureValidToken()
        delay(K.SPLASH_DELAY) // 예: 2초 대기 (필요에 따라 조정)
        navController.navigate(AllDestination.Home.route) {
            popUpTo(AllDestination.Splash.route) { inclusive = true }
        }
    }

    // 스플래시 화면 UI
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // SVG 로고 이미지 로드 및 표시
        AsyncImage(
            model = K.LOGO_IMAGE_URL,
            contentDescription = "Splash Logo",
            imageLoader = imageLoader,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(width = 304.dp, height = 141.6.dp)
        )
    }
}
