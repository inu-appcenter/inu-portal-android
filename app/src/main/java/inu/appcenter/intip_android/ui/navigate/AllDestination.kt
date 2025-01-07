package inu.appcenter.intip_android.ui.navigate

sealed class AllDestination(val route: String, val label: String) {
    data object Main : AllDestination("main", "홈")
    data object Save : AllDestination("save", "저장소")
    data object Write : AllDestination("write", "작성")
    data object MyPage : AllDestination("myPage", "마이페이지")
}