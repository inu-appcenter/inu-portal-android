package inu.appcenter.intip_android.ui.navigate

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import inu.appcenter.intip_android.WebViewScreen

@Composable
fun AppNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = AllDestination.Main.route,
        modifier = modifier
    ) {
        composable(AllDestination.Main.route) {
            WebViewScreen(url = "https://intip.inuappcenter.kr/app/home")
        }
        composable(AllDestination.Save.route) {
            WebViewScreen(url = "https://intip.inuappcenter.kr/app/save")
        }
        composable(AllDestination.Write.route) {
            WebViewScreen(url = "https://intip.inuappcenter.kr/app/write")
        }
        composable(AllDestination.MyPage.route) {
            WebViewScreen(url = "https://intip.inuappcenter.kr/app/mypage")
        }
    }
}
