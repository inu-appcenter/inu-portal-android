package inu.appcenter.intip_android.ui.component

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import inu.appcenter.intip_android.Route
import inu.appcenter.intip_android.ui.theme.INTIPTheme

@Composable
fun BottomNav() {
    //todo: 아이콘 변경
    val bottomSheetItem = listOf<Pair<ImageVector, Route>>(
        Icons.Default.Home to Route.Home,
        Icons.Default.Email to Route.Save,
        Icons.Default.Create to Route.Write,
        Icons.Default.Person to Route.MyPage
    )

    val shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, shape)
            .background(color = Color(0xffffffff), shape = shape)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        for (item in bottomSheetItem) {
            BottomItem(icon = item.first, text = item.second.label, isSelected = false)
        }
    }
}

@Composable
fun BottomItem(
    icon: ImageVector,
    text: String,
    isSelected: Boolean
) {
    val tintColor = if (isSelected) Color(0xff4071B9) else Color(0xffD6D1D5)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
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
        BottomNav()
    }
}