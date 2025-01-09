package inu.appcenter.intip_android

enum class Page(
    val label: String, // 페이지 이름
    val route : String?, // Web의 Route
) {
    Main("Main", null),
    Home("Home", "home"),
    Tips("Tips", "home/tips"),
    Save("Save", "save"),
    Write("Write", "write"),
    MyPage("MyPage", "mypage"),
}