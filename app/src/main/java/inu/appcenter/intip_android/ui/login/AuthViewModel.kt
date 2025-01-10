package inu.appcenter.intip_android.ui.login

private const val UNKNOWN_ERROR = "Unknown error"
private const val TIMEOUT_ERROR = "Signup timed out"

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

data class AuthUiState(
    val signupState: AuthState = AuthState.Idle,
    val checkNicknameState: AuthState = AuthState.Idle,
    val logoutState: AuthState = AuthState.Idle,
    val signupId: String = "",
    val signupPw: String = "",
    val saveTokenState: Boolean? = null,
    val hasToken: Boolean? = null
)