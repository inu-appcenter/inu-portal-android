package inu.appcenter.intip_android.ui.login

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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
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
    onLoginSuccess: () -> Unit,
    onOpenUrl: (String) -> Unit
) {
    // 초기 상태 리셋
    LaunchedEffect(Unit) {
        authViewModel.resetState()
    }

    val scrollState = rememberScrollState()
    val authUiState by authViewModel.uiState.collectAsState()
    val context = LocalContext.current
    var passwordVisible by remember { mutableStateOf(false) }
    val isBothFilled = authUiState.loginId.isNotBlank() && authUiState.loginPw.isNotBlank()

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
            else -> { /* Idle, Loading 등은 별도 처리 없음 */ }
        }
    }

    // 글자 크기 리소스 사용을 위한 LocalDensity 접근
    val density = LocalDensity.current
    val fontSizeSmall = with(density) { dimensionResource(id = R.dimen.font_size_small).toSp() }
    val fontSizeMedium = with(density) { dimensionResource(id = R.dimen.font_size_medium).toSp() }

    Scaffold(
        containerColor = Color.White // 항상 흰색 배경
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
                .padding(contentPadding)
                .fillMaxSize()
                .padding(dimensionResource(id = R.dimen.login_screen_padding)),
            verticalArrangement = Arrangement.Center,
        ) {
            // 로고 이미지
            AsyncImage(
                model = K.LOGO_IMAGE_URL,
                contentDescription = stringResource(id = R.string.login_logo_desc),
                imageLoader = imageLoader,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(start = dimensionResource(id = R.dimen.login_logo_padding_start))
                    .size(
                        width = dimensionResource(id = R.dimen.login_logo_width),
                        height = dimensionResource(id = R.dimen.login_logo_height)
                    )
            )
            Spacer(Modifier.height(dimensionResource(id = R.dimen.spacer_height_after_logo)))

            Text(
                text = stringResource(id = R.string.login_description),
                fontSize = fontSizeSmall
            )
            Spacer(Modifier.height(dimensionResource(id = R.dimen.spacer_height_after_login_description)))

            Text(
                text = stringResource(id = R.string.login_student_id_label),
                fontSize = fontSizeMedium
            )

            // 학번 입력 필드
            LoginPageTextField(
                value = authUiState.loginId,
                onValueChange = { authViewModel.setLoginId(it) },
                labelText = stringResource(id = R.string.login_student_id_hint),
                iconResource = R.drawable.icon_studentid,
                isPassword = false
            )
            Spacer(Modifier.height(dimensionResource(id = R.dimen.spacer_height_between_fields)))

            Text(
                text = stringResource(id = R.string.login_password_label),
                fontSize = fontSizeMedium
            )

            // 비밀번호 입력 필드
            LoginPageTextField(
                value = authUiState.loginPw,
                onValueChange = { authViewModel.setLoginPw(it) },
                labelText = stringResource(id = R.string.login_password_label),
                iconResource = R.drawable.icon_password,
                isPassword = true,
                passwordVisible = passwordVisible,
                onPasswordToggle = { passwordVisible = !passwordVisible }
            )
            Spacer(Modifier.height(dimensionResource(id = R.dimen.spacer_height_after_password_field)))

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
                text = stringResource(id = R.string.login_button_text),
                isActive = isBothFilled
            )
            Spacer(Modifier.height(dimensionResource(id = R.dimen.spacer_height_before_agreement)))

            // 약관 텍스트
            AgreementText(onOpenUrl = onOpenUrl)

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
                            Text(text = stringResource(id = R.string.confirm_text))
                        }
                    }
                }
            )
        }
    }
}
