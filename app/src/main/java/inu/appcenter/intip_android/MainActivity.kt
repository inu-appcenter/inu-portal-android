package inu.appcenter.intip_android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import inu.appcenter.intip_android.ui.page.WebPage
import inu.appcenter.intip_android.ui.theme.INTIPTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            INTIPTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = Page.Main.name,
                    enterTransition = { EnterTransition.None },
                    exitTransition = { ExitTransition.None }
                ) {
                    navigation(
                        route = Page.Main.name,
                        startDestination = Page.Home.name
                    ) {
                        composable(route = Page.Home.name) {
                            WebPage(navController = navController, page = Page.Home)
                        }
                        composable(route = Page.Save.name) {
                            WebPage(navController = navController, page = Page.Save)
                        }
                        composable(route = Page.Write.name) {
                            WebPage(navController = navController, page = Page.Write)
                        }
                        composable(route = Page.MyPage.name) {
                            WebPage(navController = navController, page = Page.MyPage)
                        }
                    }
                }
            }
        }
    }
}


//"https://intip.inuappcenter.kr/app/home"

