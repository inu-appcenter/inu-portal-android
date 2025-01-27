package inu.appcenter.intip_android.ui.navigate

sealed class AllDestination(val route: String, val label: String, val webPath: String? = null) {
    companion object {
        val webViewPage = listOf(
            Home,
            Tips, TipsNotice,  // Tips 관련 페이지들
            Menu,
            Calendar,
            Map,
            Util, UtilBook, UtilLost,  // Util 관련 페이지들
            Council, CouncilNotice, CouncilPetition, CouncilHelloBus,  // Council 관련 페이지들
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
    data object Menu : AllDestination("menu", "메뉴", "/home/menu")
    data object Calendar : AllDestination("calendar", "일정", "/home/calendar")
    data object Map : AllDestination("map", "지도", "/home/map")
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

    // Tips 관련 라우트
    data object Tips : AllDestination("tips", "Tips", "/home/tips")
    data object TipsNotice : AllDestination("tips_notice", "공지사항", "/home/tips?type=notice")
    // Util 관련 라우트
    data object Util : AllDestination("util", "편의사항", "/home/util")
    data object UtilBook : AllDestination("util_book", "도서", "/home/util?type=book")
    data object UtilLost : AllDestination("util_lost", "분실물", "/home/util?type=lost")
    // Council 관련 라우트
    data object Council : AllDestination("council", "총학", "/home/council")
    data object CouncilNotice : AllDestination("council_notice", "총학공지", "/home/council?type=notice")
    data object CouncilPetition : AllDestination("council_petition", "청원", "/home/council?type=petition")
    data object CouncilHelloBus : AllDestination("council_hellobus", "헬로버스", "/home/council?type=HelloBus")

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