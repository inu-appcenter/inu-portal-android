package inu.appcenter.intip_android.ui.navigate

sealed class AllDestination(val route: String, val label: String, val webPath: String? = null) {
    data object Home : AllDestination("home", "Home", "/home")
    data object Tips : AllDestination("tips", "Tips", "/home/tips")
    data object Menu : AllDestination("menu", "메뉴", "/home/menu")
    data object Calendar : AllDestination("calendar", "일정", "/home/calendar")
    data object Map : AllDestination("map", "지도", "/home/map")
    data object Util : AllDestination("util", "편이사항", "/home/util")
    data object Council : AllDestination("council", "총학", "/home/council")
    data object Club : AllDestination("club", "동아리", "/home/club")

    data object PostDetail : AllDestination("postDetail", "게시글 상세", "/postdetail") {
        fun createRoute(id: String) = "$route?id=$id"
    }

    data object CouncilNoticeDetail :
        AllDestination("councilNoticeDetail", "총학 공지사항", "/councilnoticedetail") {
        fun createRoute(id: String) = "$route?id=$id"
    }

    data object PetitionDetail : AllDestination("petitionDetail", "청원 상세", "/petitiondetail")
    data object Rental : AllDestination("rental", "대여", "/rental")
    data object Write : AllDestination("write", "Write", "/write")
    data object Save : AllDestination("save", "Save", "/save")

    data object MyPage : AllDestination("myPage", "Mypage", "/mypage")
    data object Profile : AllDestination("profile", "프로필", "/mypage/profile")
    data object MyPost : AllDestination("myPost", "내 게시물", "/mypage/post")
    data object MyLike : AllDestination("myLike", "내 좋아요", "/mypage/like")
    data object MyComment: AllDestination("myComment", "내 댓글", "/mypage/comment")
    data object MyDelete: AllDestination("myDelete", "탈퇴", "/mypage/delete")
    //todo: 탈퇴의 더 좋은 이름을 생각해보자

    data object Login : AllDestination("login", "로그인")
}