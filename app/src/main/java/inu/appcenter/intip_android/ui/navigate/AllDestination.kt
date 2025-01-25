package inu.appcenter.intip_android.ui.navigate

sealed class AllDestination(val route: String, val label: String, val webPath: String? = null) {
    companion object {
        // 여긴 "id 없이" 웹뷰를 로드하는 페이지 모음
        val webViewPage = listOf(
            Home,
            Tips,
            Menu,
            Calendar,
            Map,
            Util,
            Council,
            Club,
            Rental,
            Write,
            Save,
            MyPage,
            Profile,
            MyPost,
            MyLike,
            MyComment,
            MyDelete
        )
    }

    // (정적) 웹뷰 경로 예시
    data object Home : AllDestination("home", "Home", "/home")
    data object Tips : AllDestination("tips", "Tips", "/home/tips")
    data object Menu : AllDestination("menu", "메뉴", "/home/menu")
    data object Calendar : AllDestination("calendar", "일정", "/home/calendar")
    data object Map : AllDestination("map", "지도", "/home/map")
    data object Util : AllDestination("util", "편의사항", "/home/util")
    data object Council : AllDestination("council", "총학", "/home/council")
    data object Club : AllDestination("club", "동아리", "/home/club")
    data object Rental : AllDestination("rental", "대여", "/rental")
    data object Write : AllDestination("write", "Write", "/write")
    data object Save : AllDestination("save", "Save", "/save")

    data object MyPage : AllDestination("myPage", "Mypage", "/mypage")
    data object Profile : AllDestination("profile", "프로필", "/mypage/profile")
    data object MyPost : AllDestination("myPost", "내 게시물", "/mypage/post")
    data object MyLike : AllDestination("myLike", "내 좋아요", "/mypage/like")
    data object MyComment : AllDestination("myComment", "내 댓글", "/mypage/comment")
    data object MyDelete : AllDestination("myDelete", "탈퇴", "/mypage/delete")
    //todo: 탈퇴의 더 좋은 이름을 생각해보자

    // (동적) 웹뷰 경로 예시
    // route = "postDetail/{id}"에 대응
    data object PostDetail : AllDestination("postDetail", "게시글 상세", "/postdetail") {
        // 네이티브에서 이동할 때
        fun createRoute(postId: String): String = "$route/$postId"
    }

    data object CouncilNoticeDetail : AllDestination("councilNoticeDetail", "총학 공지사항", "/councilnoticedetail") {
        fun createRoute(noticeId: String): String = "$route/$noticeId"
    }

    data object PetitionDetail : AllDestination("petitionDetail", "청원 상세", "/petitiondetail") {
        fun createRoute(petitionId: String): String = "$route/$petitionId"
    }

    data object Login : AllDestination("login", "로그인")
}