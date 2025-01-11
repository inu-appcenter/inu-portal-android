package inu.appcenter.intip_android.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import inu.appcenter.intip_android.model.member.LoginDto
import inu.appcenter.intip_android.ui.component.BottomNav
import inu.appcenter.intip_android.ui.theme.INTIPTheme

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        // 첫 번째 Row - 학번 입력
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedTextField(
                value = authUiState.loginId,
                onValueChange = { authViewModel.setLoginId(it) },
                modifier = Modifier.weight(1f),
                placeholder = { Text("학번", color = Color.Gray) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            // 오른쪽 아이콘 예시
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Student ID Icon"
            )
        }

        // 두 번째 Row - 비밀번호 입력
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedTextField(
                value = authUiState.loginPw,
                onValueChange = { authViewModel.setLoginPw(it) },
                modifier = Modifier.weight(1f),
                placeholder = { Text("비밀번호", color = Color.Gray) },
                visualTransformation = PasswordVisualTransformation()
            )
            Spacer(modifier = Modifier.width(8.dp))
            // 오른쪽 아이콘 예시
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Password Icon"
            )
        }

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
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            enabled = authUiState.loginState != AuthState.Loading
        ) {
            Text(text = "로그인")
        }

        // 로딩 상태 표시 예시
        if (authUiState.loginState == AuthState.Loading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

