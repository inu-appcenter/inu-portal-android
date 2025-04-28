package inu.appcenter.intip_android

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FirebaseMessageService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.i("FCM", "onMessageReceived: ${message.notification!!.title}")
        Log.i("FCM", "onMessageReceived: ${message.notification!!.body}")
    }
}