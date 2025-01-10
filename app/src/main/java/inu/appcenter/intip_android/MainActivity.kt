package inu.appcenter.intip_android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import inu.appcenter.intip_android.ui.navigate.MyApp
import inu.appcenter.intip_android.ui.theme.INTIPTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val view = LocalView.current

            val insetsController = WindowCompat.getInsetsController(window, view)

            WindowCompat.setDecorFitsSystemWindows(window, false)
            insetsController.apply {
                hide(WindowInsetsCompat.Type.navigationBars())
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                isAppearanceLightStatusBars = true
            }

            INTIPTheme {
                MyApp()
//                NavHost(
//                    navController = navController,
//                    startDestination = Page.Main.name,
//                    enterTransition = { EnterTransition.None },
//                    exitTransition = { ExitTransition.None }
//                ) {
//                    navigation(
//                        route = Page.Main.name,
//                        startDestination = Page.Home.name
//                    ) {
//                        composable(route = Page.Home.name) {
//                            WebPage(navController = navController, page = Page.Home)
//                        }
//                        composable(route = Page.Save.name) {
//                            WebPage(navController = navController, page = Page.Save)
//                        }
//                        composable(route = Page.Write.name) {
//                            WebPage(navController = navController, page = Page.Write)
//                        }
//                        composable(route = Page.MyPage.name) {
//                            WebPage(navController = navController, page = Page.MyPage)
//                        }
//                    }
//                }
            }
        }
    }
}

//"https://intip.inuappcenter.kr/app/home"

