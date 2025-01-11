package inu.appcenter.intip_android.ui.navigate

sealed class AllDestination(val route: String, val label: String, val webPath : String? = null) {
    data object Main : AllDestination("main", "홈", "/home")
    data object Save : AllDestination("save", "저장소", "/save")
    data object Write : AllDestination("write", "작성", "/write")
    data object MyPage : AllDestination("myPage", "마이페이지", "/mypage")
    data object Login : AllDestination("login", "로그인")
}