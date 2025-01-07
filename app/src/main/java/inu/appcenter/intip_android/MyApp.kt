package inu.appcenter.intip_android

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import inu.appcenter.intip_android.ui.navigate.AllDestination
import inu.appcenter.intip_android.ui.web.WebViewScreen

@Composable
fun MyApp(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = AllDestination.Main.route,
        modifier = modifier
    ) {
        composable(AllDestination.Main.route) {
            WebViewScreen(
                navController = navController,
                path = AllDestination.Main.webPath!!
            )
        }
        composable(AllDestination.Save.route) {
            WebViewScreen(
                navController = navController,
                path = AllDestination.Save.webPath!!
            )
        }
        composable(AllDestination.Write.route) {
            WebViewScreen(
                navController = navController,
                path = AllDestination.Write.webPath!!
            )
        }
        composable(AllDestination.MyPage.route) {
            WebViewScreen(
                navController = navController,
                path = AllDestination.MyPage.webPath!!
            )
        }
    }
}