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
    text: String,
    isActive: Boolean   // 학번과 비밀번호가 모두 입력되었는지 여부 전달
) {
    // isActive가 true이면 purple 그라데이션, 아니면 기존 블루 그라데이션 적용
    val gradientColors = if (isActive) {
        listOf(
            Color(0xFF4071B9).copy(alpha = 0.7f),
            Color(0xFF4071B9).copy(alpha = 0.7f)
        )
    } else {
        listOf(
            Blue_9CAFE2.copy(alpha = 0.7f),
            Blue_B5C5F2.copy(alpha = 0.7f),
            Blue_9CAFE2.copy(alpha = 0.7f)
        )
    }

    val buttonShape = RoundedCornerShape(15.dp)

    Button(
        onClick = onClick,
        shape = buttonShape,
        enabled = isActive, // 입력이 완료되지 않으면 버튼을 비활성화할 수 있음
        modifier = Modifier
            .fillMaxWidth()
            .height(57.dp)
            .clip(buttonShape)
            .background(
                brush = Brush.linearGradient(colors = gradientColors)
            ),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 22.sp
        )
    }
}