package inu.appcenter.intip_android

object Constants {
    const val BASE_URL = "https://intip.inuappcenter.kr"
    const val USER_AGENT_SUFFIX = " INTIPApp/1.0.0"

    // 뒤로가기 제한 경로
    val RESTRICTED_PATHS = setOf("/", "/home", "/timetable", "/bus", "/chat", "/mypage")

    // 모의 수강신청 웹뷰는 뒤로가기 예외 처리가 필요한 특수 경로
    const val TIMETABLE_SIMULATOR_PATH = "/timetable/simulator"

    // 허용 도메인
    val ALLOWED_DOMAINS = listOf(BASE_URL)
}
