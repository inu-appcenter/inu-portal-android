package inu.appcenter.intip_android.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import inu.appcenter.intip_android.model.member.LoginDto
import inu.appcenter.intip_android.ui.login.util.IconTextField
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onLoginError: (String) -> Unit
) {
    val authUiState by authViewModel.uiState.collectAsState()

    // 로그인 성공/실패 감지
    LaunchedEffect(authUiState.loginState) {
        when (val state = authUiState.loginState) {
            is AuthState.Success -> {
                // 로그인 성공 시
                onLoginSuccess()
            }
            is AuthState.Error -> {
                // 로그인 실패 시
                onLoginError(state.message)
            }
            else -> {
                // Idle, Loading 등 다른 상태는 여기서 특별 조치 없음
            }
        }
    }

    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center
    ) {
        // 첫 번째 IconTextField (학번)
        IconTextField(
            value = authUiState.loginId,
            onValueChange = { authViewModel.setLoginId(it) },
            placeholder = "학번",
            leadingIcon = Icons.Default.Person
        )

        Spacer(Modifier.height(32.dp))

        // 두 번째 IconTextField (비밀번호)
        IconTextField(
            value = authUiState.loginPw,
            onValueChange = { authViewModel.setLoginPw(it) },
            placeholder = "비밀번호",
            leadingIcon = Icons.Default.Lock,
            trailingIcon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
            onTrailingIconClick = { passwordVisible = !passwordVisible },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
        )

        Spacer(Modifier.height(48.dp))

        // 로그인 버튼
        Button(
            onClick = {
                authViewModel.login(
                    LoginDto(
                        studentId = authUiState.loginId,
                        password = authUiState.loginPw
                    )
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = authUiState.loginState != AuthState.Loading
        ) {
            Text(text = "로그인")
        }

        // 로딩 상태 표시 예시
        if (authUiState.loginState == AuthState.Loading) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}