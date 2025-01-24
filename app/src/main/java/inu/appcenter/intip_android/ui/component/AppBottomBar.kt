package inu.appcenter.intip_android.ui.component

import android.util.Log
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import inu.appcenter.intip_android.R
import inu.appcenter.intip_android.ui.utils.shadowCustom
import inu.appcenter.intip_android.ui.navigate.AllDestination
import inu.appcenter.intip_android.ui.theme.Blue_9CAFE2
import inu.appcenter.intip_android.ui.theme.Gray_D6D1D5
import inu.appcenter.intip_android.ui.theme.INTIPTheme
import inu.appcenter.intip_android.ui.theme.Purple_4071B9

@Composable
fun AppBottomBar(
    navController: NavHostController
) {
    val screens = listOf<Pair<AllDestination, @DrawableRes Int>>(
        AllDestination.Main to R.drawable.icon_home_active,
        AllDestination.Save to R.drawable.icon_save,
        AllDestination.Write to R.drawable.icon_write,
        AllDestination.MyPage to R.drawable.icon_profile,
    )

    val shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    NavigationBar(
        containerColor = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, shape)
            .clip(shape)
            .background(color = Color(0xffffffff), shape = shape)
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        screens.forEach { item ->
            val screen = item.first
            val icon = item.second

            NavigationBarItem(
                modifier = Modifier
                    .drawBehind {
                        if (currentDestination?.route == screen.route) {
                            val strokeWidth =  5.dp.toPx()
                            val color = Purple_4071B9

                            drawLine(
                                color = color,
                                start = Offset(x = 64f, y = 0f),
                                end = Offset(size.width - 64f, y = 0f),
                                strokeWidth = strokeWidth
                            )
                        }
                    },
                icon = {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = screen.route,
                        modifier = Modifier.size(21.dp)
                    )
//                    Column(
//                        horizontalAlignment = Alignment.CenterHorizontally,
//                        verticalArrangement = Arrangement.spacedBy(6.89.dp) // 여기서 간격을 조절합니다
//                    ) {
//                    }
                },
                label = {
                    Text(
                        text = screen.label,
                        style = MaterialTheme.typography.labelSmall
                        //todo: 폰트 변경해야함!!
                    )
                },
                selected = currentDestination?.route == screen.route,
                onClick = {
                    if (navController.currentDestination?.route != screen.route) {
                        Log.d("AppBottomBar", "Navigating to: ${screen.route}")
                        navController.navigate(screen.route) {
                            popUpTo(AllDestination.Main.route) {
                                inclusive = true // inclusive를 true로 설정
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Blue_9CAFE2,
                    selectedTextColor = Purple_4071B9,
                    unselectedIconColor = Gray_D6D1D5,
                    unselectedTextColor = Gray_D6D1D5,
                    indicatorColor = Color.Transparent
                ),
                interactionSource = remember { MutableInteractionSource() },
            )
        }
    }
}

@Preview
@Composable
private fun AppBottomBarPrev() {
    INTIPTheme {
        AppBottomBar(navController = rememberNavController())
    }
}

fun Dp.px(density: Density): Float =
    with(density) { toPx() }