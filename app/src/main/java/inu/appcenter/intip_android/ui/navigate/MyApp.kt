package inu.appcenter.intip_android.ui.navigate

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import inu.appcenter.intip_android.ui.login.AuthViewModel
import inu.appcenter.intip_android.ui.login.LoginScreen
import inu.appcenter.intip_android.ui.screen.WebViewScreen
import android.app.Activity
import android.util.Log
import androidx.compose.ui.platform.LocalContext

@Composable
fun MyApp(authViewModel: AuthViewModel, modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val uiState by authViewModel.uiState.collectAsState()
    val context = LocalContext.current

    // 뒤로가기 처리 - Home에서만 앱 종료
    BackHandler(enabled = true) {
        if (navController.currentBackStackEntry?.destination?.route == AllDestination.Home.route) {
            (context as? Activity)?.finish()
        } else {
            navController.popBackStack()
        }
    }

    NavHost(
        navController = navController,
        startDestination = AllDestination.Home.route, // 시작점을 Home.route로 변경
        modifier = modifier
    ) {
        // 로그인 페이지
        composable(AllDestination.Login.route) {
            Log.d("NavHost", "Navigating to Login Screen")
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(AllDestination.Home.route) {
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onLoginError = { /* 에러 처리 */ },
                authViewModel = authViewModel
            )
        }

        // Home 경로를 직접 추가하여 테스트
        composable(AllDestination.Home.route) {
            WebViewScreen(
                navController = navController,
                path = AllDestination.Home.webPath,
                authViewModel = authViewModel,
                isShowBottomBar = true
            )
        }

        // 기타 정적 라우트들
        AllDestination.webViewPage.filter { it != AllDestination.Home }.forEach { destination ->
            composable(destination.route) {
                WebViewScreen(
                    navController = navController,
                    path = destination.webPath,
                    authViewModel = authViewModel,
                    isShowBottomBar = destination in listOf(
                        AllDestination.Write,
                        AllDestination.Save,
                        AllDestination.MyPage
                    )
                )
            }
        }

        // 동적 라우트들: 쿼리 필요
        composable(
            route = AllDestination.TipsSearch.routePattern,
            arguments = listOf(navArgument("search") { type = NavType.StringType })
        ) { backStackEntry ->
            val searchQuery = backStackEntry.arguments?.getString("search") ?: ""
            WebViewScreen(
                navController = navController,
                path = "${AllDestination.TipsSearch.webPath}?search=$searchQuery",
                authViewModel = authViewModel,
                isShowBottomBar = false
            )
        }

        composable(
            route = AllDestination.WriteEdit.routePattern,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("id") ?: ""
            WebViewScreen(
                navController = navController,
                path = "${AllDestination.WriteEdit.webPath}?id=$postId",
                authViewModel = authViewModel,
                isShowBottomBar = false  // 수정 페이지는 하단바 숨김
            )
        }

        composable(
            route = AllDestination.PostDetail.routePattern,
            arguments = listOf(navArgument("postId") { type = NavType.StringType })
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: ""
            WebViewScreen(
                navController = navController,
                path = "${AllDestination.PostDetail.webPath}?id=$postId",
                authViewModel = authViewModel,
                isShowBottomBar = false
            )
        }

        composable(
            route = AllDestination.CouncilNoticeDetail.routePattern,
            arguments = listOf(navArgument("noticeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val noticeId = backStackEntry.arguments?.getString("noticeId") ?: ""
            WebViewScreen(
                navController = navController,
                path = "${AllDestination.CouncilNoticeDetail.webPath}?id=$noticeId",
                authViewModel = authViewModel,
                isShowBottomBar = false
            )
        }

        composable(
            route = AllDestination.PetitionDetail.routePattern,
            arguments = listOf(navArgument("petitionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val petitionId = backStackEntry.arguments?.getString("petitionId") ?: ""
            WebViewScreen(
                navController = navController,
                path = "${AllDestination.PetitionDetail.webPath}?id=$petitionId",
                authViewModel = authViewModel,
                isShowBottomBar = false
            )
        }
    }
}
