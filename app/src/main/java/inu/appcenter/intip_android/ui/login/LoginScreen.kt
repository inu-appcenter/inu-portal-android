package inu.appcenter.intip_android.ui.login

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.CachePolicy
import inu.appcenter.intip_android.R
import inu.appcenter.intip_android.model.member.LoginDto
import inu.appcenter.intip_android.ui.login.component.AgreementText
import inu.appcenter.intip_android.ui.login.component.LoginButton
import inu.appcenter.intip_android.ui.login.component.LoginPageTextField
import inu.appcenter.intip_android.utils.K

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onLoginSuccess: () -> Unit
) {
    // 초기 상태 리셋
    LaunchedEffect(Unit) {
        authViewModel.resetState()
    }

    val scrollState = rememberScrollState()
    val authUiState by authViewModel.uiState.collectAsState()
    val context = LocalContext.current
    var passwordVisible by remember { mutableStateOf(false) }

    // SVG 디코딩을 위한 ImageLoader 구성
    val imageLoader = ImageLoader.Builder(context)
        .components { add(SvgDecoder.Factory()) }
        .diskCachePolicy(CachePolicy.ENABLED)
        .memoryCachePolicy(CachePolicy.ENABLED)
        .crossfade(true)
        .build()

    // 로그인 상태 변화 감지
    LaunchedEffect(authUiState.loginState) {
        when (authUiState.loginState) {
            is AuthState.Success -> {
                onLoginSuccess()
            }
            // AlertDialog는 여기서 바로 띄우지 않고, Composable 내 조건문으로 처리함.
            else -> { /* Idle, Loading 등은 별도 처리 없음 */ }
        }
    }

    Scaffold(
        containerColor = Color.White // 항상 흰색 배경
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
                .padding(contentPadding)
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            // 로고 이미지
            AsyncImage(
                model = K.LOGO_IMAGE_URL,
                contentDescription = "INTIP Logo",
                imageLoader = imageLoader,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(start = 6.dp)
                    .size(width = 304.dp, height = 141.6.dp)
            )
            Spacer(Modifier.height(90.dp))

            Text(text = "인천대학교 포털시스템 계정으로 로그인 할 수 있습니다.", fontSize = 12.9.sp)
            Spacer(Modifier.height(10.dp))
            Text(text = "학번", fontSize = 18.sp)

            // 학번 입력 필드
            LoginPageTextField(
                value = authUiState.loginId,
                onValueChange = { authViewModel.setLoginId(it) },
                labelText = "예) 202100000",
                iconResource = R.drawable.icon_studentid,
                isPassword = false
            )
            Spacer(Modifier.height(32.dp))
            Text(text = "비밀번호", fontSize = 18.sp)

            // 비밀번호 입력 필드
            LoginPageTextField(
                value = authUiState.loginPw,
                onValueChange = { authViewModel.setLoginPw(it) },
                labelText = "비밀번호",
                iconResource = R.drawable.icon_password,
                isPassword = true,
                passwordVisible = passwordVisible,
                onPasswordToggle = { passwordVisible = !passwordVisible }
            )
            Spacer(Modifier.height(48.dp))

            // 로그인 버튼
            LoginButton(
                onClick = {
                    authViewModel.login(
                        LoginDto(
                            studentId = authUiState.loginId,
                            password = authUiState.loginPw
                        )
                    )
                },
                text = "로그인"
            )
            Spacer(Modifier.height(16.dp))

            // 약관 텍스트
            AgreementText(onOpenUrl = { url ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            })

            // 로딩 상태 표시
            if (authUiState.loginState == AuthState.Loading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
        // 에러 상태일 때 AlertDialog 표시
        if (authUiState.loginState is AuthState.Error) {
            val errorMessage = (authUiState.loginState as AuthState.Error).message
            AlertDialog(
                onDismissRequest = { authViewModel.resetState() },
                containerColor = Color.White,
                text = {
                    Text(
                        text = errorMessage,
                        textAlign = TextAlign.Center,
                        color = Color.Black,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        TextButton(onClick = { authViewModel.resetState() }) {
                            Text(text = "확인")
                        }
                    }
                }
            )
        }
    }
}