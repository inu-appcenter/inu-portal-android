package inu.appcenter.intip_android.ui.page

import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import inu.appcenter.intip_android.Page
import inu.appcenter.intip_android.ui.component.BottomNav
import inu.appcenter.intip_android.ui.component.WebViewScreen
import inu.appcenter.intip_android.ui.theme.INTIPTheme

@Composable
fun WebPage(
    navController: NavController,
    modifier: Modifier = Modifier,
    page: Page,
) {
    //todo: 나중에 전역 변수나 env로 빼기
    val WEB_BASE_URL = "https://intip.inuappcenter.kr/app"

    Scaffold(
        bottomBar = {
            BottomNav(navController)
        },
    ) { paddingValue ->
        WebViewScreen(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = paddingValue.calculateStartPadding(LayoutDirection.Ltr),
                    end = paddingValue.calculateEndPadding(LayoutDirection.Rtl),
                    top = paddingValue.calculateTopPadding(),
                    bottom = 0.dp
                ),
            url = "${WEB_BASE_URL}/${page.route!!}"
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PagePreview() {
    INTIPTheme {
        WebPage(navController = rememberNavController(), page = Page.Home)
    }
}