package inu.appcenter.intip_android.ui.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onLoginError: (String) -> Unit
) {
    val authUiState by authViewModel.uiState.collectAsState()

}