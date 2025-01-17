package inu.appcenter.intip_android.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import inu.appcenter.intip_android.ui.navigate.Page
import inu.appcenter.intip_android.ui.theme.INTIPTheme

@Composable
fun BottomNav(navController: NavController) {
    //todo: 아이콘 변경
    val bottomSheetItem = listOf<Pair<ImageVector, Page>>(
        Icons.Default.Home to Page.Home,
        Icons.Default.Email to Page.Save,
        Icons.Default.Create to Page.Write,
        Icons.Default.Person to Page.MyPage
    )

    val shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, shape)
            .background(color = Color(0xffffffff), shape = shape),
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        for (item in bottomSheetItem) {
            BottomItem(
                modifier = Modifier.clickable {
                    navController.navigate(item.second.name)
                },
                icon = item.first,
                text = item.second.label,
                isSelected = item.second.name == navController.currentDestination?.route
            )
        }
    }
}

@Composable
fun BottomItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    text: String,
    isSelected: Boolean
) {
    val tintColor = if (isSelected) Color(0xff4071B9) else Color(0xffD6D1D5)

    Column(
        modifier = modifier
            .drawBehind {
                if (isSelected) {
                    val strokeWidth = 4.dp.toPx()
                    val color = Color(0xff4071B9)

                    drawLine(
                        color = color,
                        start = Offset(x = 0f, y = 0f),
                        end = Offset(size.width, y = 0f),
                        strokeWidth = strokeWidth
                    )
                }
            }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = tintColor,
//            modifier = Modifier.size(24.dp)
        )
        Text(
            text = text,
            color = tintColor,
            fontSize = 12.sp
        )
    }
}

@Preview(showBackground = true)
@Composable
fun BottomNavPreview() {
    INTIPTheme {
        BottomNav(rememberNavController())
    }
}