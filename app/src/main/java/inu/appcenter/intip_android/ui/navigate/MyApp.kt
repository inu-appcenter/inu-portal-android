package inu.appcenter.intip_android.ui.navigate

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import inu.appcenter.intip_android.ui.component.SplashScreen
import inu.appcenter.intip_android.ui.login.AuthViewModel
import inu.appcenter.intip_android.ui.login.LoginScreen
import inu.appcenter.intip_android.ui.screen.WebViewScreen

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
        startDestination = AllDestination.Splash.route,
        modifier = modifier
    ) {

        composable(AllDestination.Splash.route) {
            SplashScreen(navController = navController)
        }

        // 1) 로그인 페이지
        composable(AllDestination.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(AllDestination.Home.route) {
                        popUpTo(AllDestination.Login.route) { inclusive = true }
                    }
                },
                onLoginError = { /* 에러 처리 */ },
                authViewModel = authViewModel
            )
        }

        // 2) 정적 라우트들 (쿼리 없이 웹뷰만 로드)
        AllDestination.webViewPage.forEach { destination ->
            composable(destination.route) {
                WebViewScreen(
                    navController = navController,
                    path = destination.webPath,
                    authViewModel = authViewModel,
                    isShowBottomBar = destination in listOf(
                        AllDestination.Home,
                        AllDestination.Write,
                        AllDestination.Save,
                        AllDestination.MyPage
                    )
                )
            }
        }

        // 3) 동적 라우트들: 쿼리 필요
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
            route = "postDetail/{postId}",
            arguments = listOf(navArgument("postId") { type = NavType.StringType })
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: ""
            WebViewScreen(
                navController = navController,
                // 실제 웹뷰 로드 URL = "/postdetail?id=postId"
                path = "${AllDestination.PostDetail.webPath}?id=$postId",
                authViewModel = authViewModel,
                isShowBottomBar = false
            )
        }

        composable(
            route = "councilNoticeDetail/{noticeId}",
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
            route = "petitionDetail/{petitionId}",
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