package inu.appcenter.intip_android.ui.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import inu.appcenter.intip_android.local.DataStoreManager
import inu.appcenter.intip_android.model.member.LoginDto
import inu.appcenter.intip_android.repository.member.MemberRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeParseException

private const val UNKNOWN_ERROR_MESSAGE = "Unknown error"

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

data class AuthUiState(
    val loginState: AuthState = AuthState.Idle,
    val logoutState: AuthState = AuthState.Idle,
    val loginId: String = "",
    val loginPw: String = "",
    val hasToken: Boolean? = null,
    val token: String? = null,
    val hasShownTokenAlert: Boolean = false  // 토큰 알림 표시 여부
)

class AuthViewModel(
    private val memberRepository: MemberRepository,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        // 앱 시작 시 토큰 상태를 단순히 관찰해서 UI에 반영만 함
        observeToken()
    }

    private fun observeToken() {
        viewModelScope.launch {
            combine(
                dataStoreManager.accessToken,
                dataStoreManager.accessTokenExpiredTime
            ) { token, expiry ->
                val valid = token != null && expiry != null && !isAccessTokenExpired(expiry)
                Pair(valid, token)
            }.collect { (valid, token) ->
                _uiState.update { it.copy(hasToken = valid, token = token) }
                Log.d("AuthViewModel", "유효 토큰 여부: $valid, Token: $token")
            }
        }
    }

    fun setLoginId(loginId: String) {
        _uiState.update { it.copy(loginId = loginId) }
    }

    fun setLoginPw(loginPw: String) {
        _uiState.update { it.copy(loginPw = loginPw) }
    }

    fun setTokenAlertShown() {
        _uiState.update { it.copy(hasShownTokenAlert = true) }
    }

    /**
     * 토큰 만료 여부 확인 함수
     * expiredTimeString (예: "2025-01-22T23:25:47.754524713")을 파싱하여 현재 시간과 비교합니다.
     */
    private fun isAccessTokenExpired(expiredTimeString: String): Boolean {
        return try {
            val expiry = LocalDateTime.parse(expiredTimeString)
            LocalDateTime.now().isAfter(expiry)
        } catch (e: DateTimeParseException) {
            true
        }
    }

    /**
     * refreshToken 함수
     * 저장된 refresh token 정보를 기반으로 새로운 access token과 refresh token을 받아옵니다.
     * (서버에서 두 토큰을 모두 재발급한다고 가정)
     */
    private suspend fun refreshToken(): Boolean {
        val currentRefreshToken = dataStoreManager.refreshToken.first()
        val currentRefreshExpiry = dataStoreManager.refreshTokenExpiredTime.first()

        if (currentRefreshToken.isNullOrEmpty() ||
            currentRefreshExpiry.isNullOrEmpty() ||
            isAccessTokenExpired(currentRefreshExpiry)
        ) {
            return false
        }

        val response = memberRepository.refreshToken(currentRefreshToken)
        if (response.isSuccessful) {
            val loginResponse = response.body() ?: throw Exception("토큰 갱신 응답이 비어있습니다.")
            val tokenDto = loginResponse.data

            dataStoreManager.saveAccessToken(tokenDto.accessToken, tokenDto.accessTokenExpiredTime)
            dataStoreManager.saveRefreshToken(tokenDto.refreshToken, tokenDto.refreshTokenExpiredTime)
            Log.d("AuthViewModel", "토큰 재발급 성공, 새 토큰: ${tokenDto.accessToken}")
            return true
        }
        return false
    }

    suspend fun ensureValidToken() {
        val currentToken = dataStoreManager.accessToken.first()
        val currentTokenExpiry = dataStoreManager.accessTokenExpiredTime.first()
        if (currentToken != null && currentTokenExpiry != null) {
            if (isAccessTokenExpired(currentTokenExpiry)) {
                Log.d("AuthViewModel", "Access token expired, attempting to refresh...")
                refreshToken()  // 토큰 갱신 시도 (refreshToken은 이미 구현되어 있음)
            }
        }
    }

    /**
     * safeApiCall 함수
     * API 호출 후 토큰 만료 응답(예: HTTP 401)을 감지하면 refreshToken()을 호출하고,
     * 새 토큰으로 동일 API를 재요청합니다.
     */
    private suspend fun <T> safeApiCall(apiCall: suspend () -> retrofit2.Response<T>): retrofit2.Response<T> {
        var response = apiCall()
        if (!response.isSuccessful && isTokenExpiredResponse(response)) {
            val refreshSuccess = refreshToken()
            if (refreshSuccess) {
                response = apiCall()
            }
        }
        return response
    }

    /**
     * 서버 응답이 토큰 만료를 나타내는지 판단하는 함수
     * 예시로 HTTP 401 코드나 errorBody의 특정 문구를 확인합니다.
     */
    private fun <T> isTokenExpiredResponse(response: retrofit2.Response<T>): Boolean {
        if (response.code() == 401) return true
        val errorBody = response.errorBody()?.string() ?: ""
        return errorBody.contains("토큰 만료") || errorBody.contains("만료")
    }

    /**
     * 로그인 함수
     * 앱 진입 시, 저장된 토큰의 유효성을 먼저 확인합니다.
     * 토큰이 유효하면 바로 로그인 성공으로 처리하고,
     * 토큰이 없거나 만료된 경우 refreshToken()을 시도한 후,
     * 그래도 없으면 로그인 API를 호출합니다.
     */
    fun login(loginDto: LoginDto) {
        Log.d("AuthViewModel", "login() 호출")
        viewModelScope.launch {
            _uiState.update { it.copy(loginState = AuthState.Loading) }
            try {
                // 앱 진입 시 저장된 토큰이 있는 경우 유효성 검사
                val currentToken = dataStoreManager.accessToken.first()
                val currentTokenExpiry = dataStoreManager.accessTokenExpiredTime.first()
                if (currentToken != null && currentTokenExpiry != null) {
                    if (!isAccessTokenExpired(currentTokenExpiry)) {
                        // 유효한 토큰이 있으면 바로 성공 처리
                        _uiState.update { it.copy(loginState = AuthState.Success) }
                        return@launch
                    }
                    // 저장된 토큰이 만료된 경우 refreshToken() 시도
                    val refreshSuccess = refreshToken()
                    if (refreshSuccess) {
                        _uiState.update { it.copy(loginState = AuthState.Success) }
                        return@launch
                    }
                }

                // 저장된 토큰이 없거나 refresh에 실패한 경우, 로그인 API 호출을 safeApiCall으로 처리
                val response = safeApiCall { memberRepository.login(loginDto) }
                if (response.isSuccessful) {
                    val loginResponse = response.body() ?: throw Exception("로그인 응답이 비어있습니다.")
                    val tokenDto = loginResponse.data
                    dataStoreManager.saveAccessToken(tokenDto.accessToken, tokenDto.accessTokenExpiredTime)
                    dataStoreManager.saveRefreshToken(tokenDto.refreshToken, tokenDto.refreshTokenExpiredTime)
                    _uiState.update { it.copy(loginState = AuthState.Success) }
                } else {
                    throw Exception(response.errorBody()?.string() ?: "알 수 없는 에러")
                }
            } catch (e: Exception) {
                Log.e("login", e.message ?: "error")
                val errorMessage = e.message?.let { extractErrorMessage(it) } ?: UNKNOWN_ERROR_MESSAGE
                _uiState.update { it.copy(loginState = AuthState.Error(errorMessage)) }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.update { it.copy(logoutState = AuthState.Loading) }
            try {
                dataStoreManager.clearTokens()
                _uiState.update { it.copy(logoutState = AuthState.Success, hasToken = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(logoutState = AuthState.Error(e.message ?: UNKNOWN_ERROR_MESSAGE)) }
            }
        }
    }

    fun resetState() {
        _uiState.update {
            it.copy(
                loginState = AuthState.Idle,
                logoutState = AuthState.Idle,
                loginId = "",
                loginPw = "",
                token = null,
                hasShownTokenAlert = false
            )
        }
    }

    private fun extractErrorMessage(message: String): String {
        val regex = """"msg"\s*:\s*"([^"]*)"""".toRegex()
        val matchResult = regex.find(message)
        return matchResult?.groupValues?.get(1) ?: message
    }
}
