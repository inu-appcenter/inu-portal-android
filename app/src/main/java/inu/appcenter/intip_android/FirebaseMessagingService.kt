package inu.appcenter.intip_android

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        android.util.Log.d("FCM_LOG", "Refreshed token: $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        android.util.Log.d("FCM_LOG", "From: ${remoteMessage.from}")
        android.util.Log.d("FCM_LOG", "Notification Message Body: ${remoteMessage.notification?.body}")
        android.util.Log.d("FCM_LOG", "Data Payload: ${remoteMessage.data}")

        // 알림 권한 체크 (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (!notificationManager.areNotificationsEnabled()) {
                android.util.Log.w("FCM_LOG", "Notifications are disabled by the user.")
                return
            }
        }

        // 알림 채널 생성
        createNotificationChannel()

        // 알림 내용 설정
        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "알림"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: "새로운 알림이 도착했습니다."
        val targetPath = remoteMessage.data["TARGET_PATH"]

        // 알림 클릭 시 실행될 Intent 설정
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            if (targetPath != null) {
                putExtra("TARGET_PATH", targetPath)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 알림 생성 (채널 ID: fcm_default_channel)
        val notificationBuilder = NotificationCompat.Builder(this, "fcm_default_channel")
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_notification_logo)
            .setColor(androidx.core.content.ContextCompat.getColor(this, R.color.intip_color))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)

        // LargeIcon 설정 (알림 오른쪽 혹은 왼쪽에 표시되는 앱 로고)
        val largeIcon = android.graphics.BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
        notificationBuilder.setLargeIcon(largeIcon)

        // 알림 표시
        try {
            val notificationManager = NotificationManagerCompat.from(this)
            notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
            android.util.Log.d("FCM_LOG", "Notification displayed successfully.")
        } catch (e: SecurityException) {
            android.util.Log.e("FCM_LOG", "SecurityException: ${e.message}")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "일반 알림"
            val descriptionText = "앱의 기본적인 알림을 수신합니다."
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("fcm_default_channel", name, importance).apply {
                description = descriptionText
                enableLights(true)
                enableVibration(true)
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}