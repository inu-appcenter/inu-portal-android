package inu.appcenter.intip_android.ui.login.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import inu.appcenter.intip_android.ui.theme.Gray_969696
import inu.appcenter.intip_android.ui.theme.Gray_BDBDBD

@Composable
fun LoginPageTextField(
    value: String,
    onValueChange: (String) -> Unit,
    labelText: String,
    iconResource: Int,                // 아이콘 리소스 (학번/비번 각각 다른 XML 사용)
    isPassword: Boolean = false,      // 비밀번호 여부
    passwordVisible: Boolean = false, // 비밀번호가 보이는 상태인지
    onPasswordToggle: (() -> Unit)? = null // 토글 콜백
) {
    // 비밀번호 시각 변환 설정
    val visualTransformation = if (isPassword && !passwordVisible) {
        PasswordVisualTransformation()
    } else {
        VisualTransformation.None
    }

    // OutlinedTextField 사용
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = labelText,
                color = Gray_BDBDBD
            )
        },
        visualTransformation = visualTransformation,
        // 비밀번호 아이콘 혹은 일반 trailing 아이콘
        trailingIcon = {
            if (isPassword) {
                IconButton(
                    onClick = { onPasswordToggle?.invoke() }
                ) {
                    Icon(
                        painter = painterResource(id = iconResource),
                        contentDescription = null,
                        tint = Gray_969696
                    )
                }
            } else {
                IconButton(
                    onClick = { }
                ) {
                    Icon(
                        painter = painterResource(id = iconResource),
                        contentDescription = null,
                        tint = Gray_969696
                    )
                }
            }
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            disabledBorderColor = Color.Transparent,
            errorBorderColor = Color.Transparent,
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black,
            disabledTextColor = Color.Black,
            errorTextColor = Color.Black,
            cursorColor = Color.Black,
        )
    )
    HorizontalDivider(
        thickness = 1.dp,
        color = Gray_969696
    )
}
