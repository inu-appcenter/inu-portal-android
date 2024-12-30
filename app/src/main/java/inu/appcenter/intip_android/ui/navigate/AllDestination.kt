package inu.appcenter.intip_android.ui.navigate

import inu.appcenter.intip_android.R

sealed class AllDestination(val route: String, val icon: Int, val label: String) {
    data object Main : AllDestination("main", R.drawable.icon_home_active, "홈")
    data object Save : AllDestination("save", R.drawable.icon_save, "저장소")
    data object Write : AllDestination("write", R.drawable.icon_write, "작성")
    data object MyPage : AllDestination("myPage", R.drawable.icon_profile, "마이페이지")
}