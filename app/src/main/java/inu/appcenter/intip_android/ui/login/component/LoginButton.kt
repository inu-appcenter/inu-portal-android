package inu.appcenter.intip_android.ui.login.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import inu.appcenter.intip_android.ui.theme.Blue_9CAFE2
import inu.appcenter.intip_android.ui.theme.Blue_B5C5F2

@Composable
fun LoginButton(
    onClick: () -> Unit,
    text: String
) {
    // Figma에서 정의한 색상 값 (70% 불투명도의 색상)
    // Hex 코드에 투명도 값(B2)이 포함되어 있으므로, 별도로 alpha를 조정할 필요 없이 사용합니다.
    val gradientColors = listOf(
        Blue_9CAFE2.copy(alpha = 0.7f), // 시작 색상
        Blue_B5C5F2.copy(alpha = 0.7f), // 중간 색상
        Blue_9CAFE2.copy(alpha = 0.7f)  // 마지막 색상
    )

    val buttonShape = RoundedCornerShape(15.dp)

    Button(
        onClick = onClick,
        shape = buttonShape,
        modifier = Modifier
            .fillMaxWidth()
            .height(57.dp)
            .clip(buttonShape)
            // 버튼 내부 영역에 그라데이션을 적용합니다.
            .background(
                brush = Brush.linearGradient(colors = gradientColors)
            ),
        // backgroundColor를 투명하게 하여 Modifier의 배경이 보이도록 합니다.
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 22.sp
        )
    }
}