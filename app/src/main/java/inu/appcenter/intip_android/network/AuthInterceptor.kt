package inu.appcenter.intip_android.network

import android.util.Log
import inu.appcenter.intip_android.local.DataStoreManager
import inu.appcenter.intip_android.utils.K
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val dataStoreManager: DataStoreManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url
        val path = url.encodedPath

        Log.d("AuthInterceptor", "URL: $url, Path: $path, REFRESH: ${K.REFRESH}")

        // 토큰 갱신 요청을 인터셉트에서 제외합니다.
        if (path.endsWith(K.REFRESH)) {
            Log.d("AuthInterceptor", "토큰 갱신 요청 감지: 인터셉트 제외")
            return chain.proceed(request)
        }

        val token = runBlocking { dataStoreManager.accessToken.first() }
        Log.d("AuthInterceptor", "Using token: $token") // 토큰 로그 출력
        val newRequest = request.newBuilder()
            .apply {
                token?.let {
                    addHeader("Authorization", "Bearer $it")
                }
            }
            .build()
        return chain.proceed(newRequest)
    }
}