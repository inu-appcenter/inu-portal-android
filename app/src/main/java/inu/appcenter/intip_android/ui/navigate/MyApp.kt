package inu.appcenter.intip_android.ui.navigate

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import inu.appcenter.intip_android.ui.login.AuthViewModel
import inu.appcenter.intip_android.ui.login.LoginScreen
import inu.appcenter.intip_android.ui.screen.SplashScreen
import inu.appcenter.intip_android.ui.screen.WebViewScreen
import inu.appcenter.intip_android.utils.K

@Composable
fun MyApp(authViewModel: AuthViewModel, modifier: Modifier = Modifier) {
    val navController = rememberNavController()
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
        startDestination = AllDestination.Splash.route,
        modifier = modifier
    ) {

        composable(AllDestination.Splash.route) {
            SplashScreen(
                onSplashComplete = {
                    navController.navigate(AllDestination.Home.route) {
                        popUpTo(AllDestination.Splash.route) { inclusive = true }
                    }
                },
                authViewModel = authViewModel
            )
        }

        // 1) 로그인 페이지
        composable(AllDestination.Login.route) {
            LoginScreen(
                authViewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate(AllDestination.Home.route) {
                        popUpTo(AllDestination.Login.route) { inclusive = true }
                    }
                    authViewModel.getFCMToken()
                },
                onOpenUrl = { url ->
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                }
            )
        }

        // 2) 정적 라우트들 (쿼리 없이 웹뷰만 로드)
        AllDestination.webViewPage.forEach { destination ->
            composable(destination.route) {
                WebViewScreen(
                    navController = navController,
                    path = destination.webPath,
                    authViewModel = authViewModel,
                    isShowBottomBar = destination in K.BOTTOM_BAR_DESTINATIONS
                )
            }
        }

        // 3) 동적 라우트들: 쿼리 필요
        composable(
            route = AllDestination.UtilBookSearch.routePattern,
            arguments = listOf(navArgument("search") { type = NavType.StringType })
        ) { backStackEntry ->
            val searchQuery = backStackEntry.arguments?.getString("search") ?: ""
            WebViewScreen(
                navController = navController,
                path = "${AllDestination.UtilBookSearch.webPath}?type=book&search=$searchQuery",
                authViewModel = authViewModel,
                isShowBottomBar = false
            )
        }

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
                // 실제 웹뷰 로드 URL = "/postdetail?id=postId"
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

        composable(
            route = AllDestination.RecruitDetail.routePattern,
            arguments = listOf(
                navArgument("id") { type = NavType.StringType },
                navArgument("name") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: ""
            val name = backStackEntry.arguments?.getString("name") ?: ""
            WebViewScreen(
                navController = navController,
                path = "${AllDestination.RecruitDetail.webPath}?id=$id&name=$name",
                authViewModel = authViewModel,
                isShowBottomBar = false
            )
        }
    }
}