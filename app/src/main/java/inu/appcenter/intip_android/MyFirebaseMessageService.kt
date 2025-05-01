package inu.appcenter.intip_android

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessageService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title
        val body  = message.notification?.body

        Log.i("FCM", "onMessageReceived: $title, $body")

        if(title != null && body != null) {
            this.showNotification(title, body)
        }
    }
}