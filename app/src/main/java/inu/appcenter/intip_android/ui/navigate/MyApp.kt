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

//    // 커스텀 네비게이션 스택을 추적하기 위한 리스트
//    val navigationStack = remember { mutableListOf<String>() }
//
//    // 네비게이션 스택 로그를 출력하기 위한 리스너 등록
//    DisposableEffect(navController) {
//        val listener = NavController.OnDestinationChangedListener { controller, destination, arguments ->
//            handleDestinationChange(navigationStack, destination)
//            Log.d("NavigationStack", "Current Stack: $navigationStack")
//        }
//        navController.addOnDestinationChangedListener(listener)
//
//        // 컴포저블이 사라질 때 리스너 제거
//        onDispose {
//            navController.removeOnDestinationChangedListener(listener)
//        }
//    }

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
                path = AllDestination.Main.webPath!!,
                authViewModel = authViewModel
            )
        }
        composable(AllDestination.Save.route) {
            WebViewScreen(
                navController = navController,
                path = AllDestination.Save.webPath!!,
                authViewModel = authViewModel
            )
        }
        composable(AllDestination.Write.route) {
            WebViewScreen(
                navController = navController,
                path = AllDestination.Write.webPath!!,
                authViewModel = authViewModel
            )
        }
        composable(AllDestination.MyPage.route) {
            WebViewScreen(
                navController = navController,
                path = AllDestination.MyPage.webPath!!,
                authViewModel = authViewModel
            )
        }
    }
}

///**
// * 네비게이션 목적지 변경 시 스택을 업데이트하는 함수
// */
//private fun handleDestinationChange(stack: MutableList<String>, destination: NavDestination) {
//    val route = destination.route ?: "unknown"
//
//    // 스택에 이미 존재하는 경로라면 해당 경로 이후의 모든 경로를 제거
//    val existingIndex = stack.indexOf(route)
//    if (existingIndex != -1) {
//        // 현재 목적지가 스택에 이미 존재하면 해당 위치 이후를 제거
//        while (stack.size > existingIndex + 1) {
//            stack.removeAt(stack.size - 1)
//        }
//    } else {
//        // 새로운 목적지라면 스택에 추가
//        stack.add(route)
//    }
//}