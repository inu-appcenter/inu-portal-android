package inu.appcenter.intip_android.utils

import androidx.compose.ui.unit.dp
import inu.appcenter.intip_android.ui.navigate.AllDestination

object K {
    // Base URLs
    const val BASE_URL = "https://portal.inuappcenter.kr/"
    const val WEB_BASE_URL = "https://intip.inuappcenter.kr/app"

    // API Endpoints
    const val LOGIN = "/api/members/login"
    const val REFRESH = "/api/members/refresh"

    const val FCM_TOKEN_UPDATE = "/api/tokens"

    // DataStore Keys
    const val DATASTORE_NAME = "user_prefs"
    const val ACCESS_TOKEN = "access_token_key"
    const val ACCESS_TOKEN_EXPIRED_TIME = "access_token_expired_time_key"
    const val REFRESH_TOKEN = "refresh_token_key"
    const val REFRESH_TOKEN_EXPIRED_TIME = "refresh_token_expired_time_key"

    // 외부 링크 상수 (이용약관, 개인정보 처리지침)
    const val TERMS_OF_USE_URL = "https://intip.inuappcenter.kr/terms-of-use.html"
    const val PRIVACY_POLICY_URL = "https://intip.inuappcenter.kr/privacy-policy.html"

    // 스플래시 화면 및 로그인 관련 상수
    const val SPLASH_DELAY = 2000L
    const val LOGO_IMAGE_URL = "https://intip.inuappcenter.kr/assets/logo-with-text-FfH8Z-gk.svg"

    // BottomBar 관련 상수 (BottomBar를 보여줄 목적지 목록)
    val BOTTOM_BAR_DESTINATIONS = listOf(AllDestination.Home, AllDestination.Write, AllDestination.Save, AllDestination.MyPage)
}