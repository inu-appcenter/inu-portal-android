package inu.appcenter.intip_android.ui.login

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.CachePolicy
import inu.appcenter.intip_android.R
import inu.appcenter.intip_android.model.member.LoginDto
import inu.appcenter.intip_android.ui.login.util.AgreementText
import inu.appcenter.intip_android.ui.login.util.LoginButton
import inu.appcenter.intip_android.ui.login.util.LoginPageTextField

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onLoginError: (String) -> Unit
) {
    LaunchedEffect(Unit) {
        authViewModel.resetState()
    }

    val authUiState by authViewModel.uiState.collectAsState()
    val context = LocalContext.current
    var passwordVisible by remember { mutableStateOf(false) }

    // SVG 디코딩을 위한 ImageLoader 구성
    val imageLoader = ImageLoader.Builder(context)
        .components { add(SvgDecoder.Factory()) }
        .diskCachePolicy(CachePolicy.ENABLED)  // 디스크 캐시 활성화
        .memoryCachePolicy(CachePolicy.ENABLED) // 메모리 캐시 활성화
        .crossfade(true)
        .build()

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
            .padding(32.dp),
        verticalArrangement = Arrangement.Center
    ) {
        //가로 304 세로 141.6
        AsyncImage(
            model = "https://intip.inuappcenter.kr/assets/logo-with-text-FfH8Z-gk.svg",
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

        // 첫 번째 IconTextField (학번)
        LoginPageTextField(
            value = authUiState.loginId,
            onValueChange = { authViewModel.setLoginId(it) },
            labelText = "예) 202100000",
            iconResource = R.drawable.icon_studentid, // 학번 아이콘
            isPassword = false  // 일반 텍스트 필드
        )

        Spacer(Modifier.height(32.dp))

        Text(text = "비밀번호", fontSize = 18.sp)

        // 두 번째 IconTextField (비밀번호)
        LoginPageTextField(
            value = authUiState.loginPw,
            onValueChange = { authViewModel.setLoginPw(it) },
            labelText = "비밀번호",
            iconResource = R.drawable.icon_password, // 비밀번호 아이콘
            isPassword = true,
            passwordVisible = passwordVisible,
            onPasswordToggle = { passwordVisible = !passwordVisible }
        )

        Spacer(Modifier.height(48.dp))

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

        AgreementText(onOpenUrl = { url ->
            // 클릭한 URL을 Intent로 처리합니다.
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        })

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