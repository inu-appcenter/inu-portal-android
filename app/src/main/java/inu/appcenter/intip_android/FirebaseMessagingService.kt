package inu.appcenter.intip_android

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onMessageReceived(remoteMessage: RemoteMessage) {

        // FCM 데이터 메시지에서 TARGET_PATH 값 추출
        val targetPath = remoteMessage.data["TARGET_PATH"]

        if (targetPath != null) {

            // 앱이 종료된 상태에서 알림을 클릭하면 MainActivity로 이동하도록 설정
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra("TARGET_PATH", targetPath)
            }

            // 알림 클릭 시 실행될 PendingIntent 생성
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // 알림 생성
            val notification = NotificationCompat.Builder(this, "default")
                .setContentTitle("알림")
                .setContentText("알림을 클릭하면 해당 경로로 이동합니다.")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            // 알림 표시
            NotificationManagerCompat.from(this).notify(1, notification)
        }
    }
}