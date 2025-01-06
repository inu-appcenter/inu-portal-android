package inu.appcenter.intip_android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import inu.appcenter.intip_android.ui.page.WebPage
import inu.appcenter.intip_android.ui.theme.INTIPTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            INTIPTheme {
                WebPage(page = Page.Home)
            }
        }
    }
}


//"https://intip.inuappcenter.kr/app/home"

