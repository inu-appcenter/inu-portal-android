package inu.appcenter.intip_android.service

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import inu.appcenter.intip_android.utils.FcmTokenHolder

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token: $token")
        // Get new FCM registration token
        FcmTokenHolder.token = token
    }
}
