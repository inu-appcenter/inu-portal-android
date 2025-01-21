package inu.appcenter.intip_android.ui.navigate

sealed class AllDestination(val route: String, val label: String, val webPath : String? = null) {
    data object Main : AllDestination("main", "Home", "/home")
    data object Save : AllDestination("save", "Save", "/save")
    data object Write : AllDestination("write", "Write", "/write")
    data object MyPage : AllDestination("myPage", "Mypage", "/mypage")
    data object Login : AllDestination("login", "로그인")
}