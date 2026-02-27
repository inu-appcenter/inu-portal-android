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
        val targetPath = remoteMessage.data["TARGET_PATH"]
        if (targetPath != null) {
            // ??Ï¢ÖÎ£å ?ÅÌÉú?êÏÑú ?åÎ¶º ?¥Î¶≠ ??MainActivityÎ°??ÑÎã¨
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra("TARGET_PATH", targetPath)
            }
            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(this, "default")
                .setContentTitle("?åÎ¶º")
                .setContentText("?åÎ¶º ?¥Î¶≠ ??Í≤ΩÎ°ú ?¥Îèô")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            NotificationManagerCompat.from(this).notify(1, notification)
        }
    }
}
