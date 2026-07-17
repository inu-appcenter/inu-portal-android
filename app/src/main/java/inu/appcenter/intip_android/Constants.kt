package inu.appcenter.intip_android

object Constants {
    val BASE_URL = BuildConfig.BASE_URL
    const val USER_AGENT_SUFFIX = " INTIPApp/1.0.0"
    const val SIMULATOR_BASE_URL = "https://inu-sugang-simulator.pages.dev"

    // 뒤로가기 제한 경로
    val RESTRICTED_PATHS = setOf("/", "/home", "/timetable", "/bus", "/chat", "/mypage")

    // 모의 수강신청 화면의 뒤로가기 예외 경로
    const val TIMETABLE_SIMULATOR_PATH = "/timetable/simulator"

    // 허용 도메인
    val ALLOWED_DOMAINS = listOf(BASE_URL, SIMULATOR_BASE_URL)
}
