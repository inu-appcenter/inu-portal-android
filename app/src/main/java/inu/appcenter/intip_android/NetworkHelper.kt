package inu.appcenter.intip_android

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

class NetworkHelper(private val context: Context) {

    // 현재 기기가 인터넷에 연결되어 있는지 여부를 확인하는 함수
    fun isInternetAvailable(): Boolean {

        // 시스템의 네트워크 서비스 객체를 가져옴
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // 현재 활성화된 네트워크를 가져오며, 없으면 false 반환
        val network = cm.activeNetwork ?: return false

        // 해당 네트워크의 세부 기능 정보를 가져오며, 없으면 false 반환
        val capabilities = cm.getNetworkCapabilities(network) ?: return false

        // 네트워크가 인터넷 사용 가능 기능을 가지고 있는지 확인하여 결과 반환
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}