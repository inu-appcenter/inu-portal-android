package inu.appcenter.intip_android

enum class Page(
    val navigation: String, // 안드로이드 Navigation
    val label: String, // 페이지 이름
    val route : String, // Web의 Route
) {
    Home("home", "Home", "home"),
    Tips("tips", "Tips", "home/tips"),
    Save("save", "Save", "save"),
    Write("write", "Write", "write"),
    MyPage("mypage", "MyPage", "mypage"),
}