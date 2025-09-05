package inu.appcenter.intip_android.utils

import android.content.Context
import android.util.Log
import android.webkit.JavascriptInterface

class WebAppInterface(private val context: Context) {

    /**
     * Web에서 FCM 토큰을 요청할 때 호출됩니다.
     * @return 현재 기기의 FCM 토큰
     */
    @JavascriptInterface
    fun getFcmToken(): String? {
        Log.d("WebAppInterface", "getFcmToken() called from web. Returning: ${FcmTokenHolder.token}")
        return FcmTokenHolder.token
    }

    /**
     * Web에서 로그인이 성공한 후 토큰 저장을 요청할 때 호출됩니다.
     * @param token 저장할 FCM 토큰
     */
    @JavascriptInterface
    fun saveFcmToken(token: String) {
        Log.d("WebAppInterface", "saveFcmToken() called from web with token: $token")

        // SharedPreferences에 토큰 저장
        val sharedPref = context.getSharedPreferences("FCM_PREF", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("FCM_TOKEN", token)
            apply()
        }
        Log.i("WebAppInterface", "FCM Token saved successfully.")
    }
}
