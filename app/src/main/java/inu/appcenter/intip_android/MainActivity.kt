package inu.appcenter.intip_android

import android.content.pm.PackageManager
import android.os.Build
import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.firebase.messaging.FirebaseMessaging
import inu.appcenter.intip_android.ui.login.AuthViewModel
import inu.appcenter.intip_android.ui.navigate.MyApp
import inu.appcenter.intip_android.ui.theme.INTIPTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    private val authViewModel : AuthViewModel by viewModel()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {

            val view = LocalView.current

            val insetsController = WindowCompat.getInsetsController(window, view)

            WindowCompat.setDecorFitsSystemWindows(window, true)
            insetsController.apply {
                hide(WindowInsetsCompat.Type.navigationBars())
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                isAppearanceLightStatusBars = true
            }

            INTIPTheme {
                MyApp(authViewModel = authViewModel)
            }
        }
    }

    override fun onStart() {
        super.onStart()

        requestNotificationPermission()
        getFCMToken()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13 이상
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }
    }

    private fun getFCMToken() {
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                    return@addOnCompleteListener
                }

                val token = task.result
                Log.d("FCM", "FCM token: $token")
                authViewModel.updateFCMToken(token)

            }

        FirebaseMessaging.getInstance().subscribeToTopic("notice")
            .addOnCompleteListener { task ->
                if(task.isSuccessful) {
                    Log.d("FCM", "topics 구독 완료")
                }
                else {
                    Log.e("FCM", "topics 구독 실패")
                }
            }
    }
}

