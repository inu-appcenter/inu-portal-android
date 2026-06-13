package inu.appcenter.intip_android

object Constants {
    val BASE_URL = BuildConfig.BASE_URL
    const val USER_AGENT_SUFFIX = " INTIPApp/1.0.0"

    // 뒤로가기 제한 경로
    val RESTRICTED_PATHS = setOf("/", "/home", "/timetable", "/bus", "/chat", "/mypage")

    // 허용 도메인
    val ALLOWED_DOMAINS = listOf(BASE_URL)
}
