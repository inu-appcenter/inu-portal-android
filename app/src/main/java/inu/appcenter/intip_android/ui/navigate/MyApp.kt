package inu.appcenter.intip_android.ui.navigate

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import inu.appcenter.intip_android.ui.login.AuthViewModel
import inu.appcenter.intip_android.ui.login.LoginScreen
import inu.appcenter.intip_android.ui.screen.WebViewScreen

@Composable
fun MyApp(authViewModel: AuthViewModel, modifier: Modifier = Modifier) {

    val navController = rememberNavController()
    val uiState by authViewModel.uiState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = if(uiState.hasToken == true) AllDestination.Main.route else AllDestination.Login.route,
        modifier = modifier
    ) {
        composable(AllDestination.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(AllDestination.Main.route) {
                        popUpTo(AllDestination.Login.route) { inclusive = true }
                    }
                },
                onLoginError = {
                },
                authViewModel = authViewModel
            )
        }
        //todo: 나중에 WebViewScreen들은 반복문으로 관리하면 좋을듯
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