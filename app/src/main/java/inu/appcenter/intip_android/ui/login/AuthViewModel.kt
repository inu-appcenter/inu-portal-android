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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeParseException

private const val UNKNOWN_ERROR = "Unknown error"

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
    val token: String? = null
)

class AuthViewModel(
    private val memberRepository: MemberRepository,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // tokenFlow를 외부에 공개합니다.
    val tokenFlow = dataStoreManager.accessToken

    init {
        observeToken()

        viewModelScope.launch {
            dataStoreManager.accessToken.collect { token ->
                Log.d("AuthViewModel", "Flow 업데이트, accessToken: $token")
            }
        }
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


    /**
     * 토큰 만료여부 확인 함수
     * 파라미터로 들어온 expiredTimeString (예: "2025-01-22T23:25:47.754524713")을 LocalDateTime으로 파싱하여 현재시간과 비교합니다.
     */
    private fun isAccessTokenExpired(expiredTimeString: String): Boolean {
        return try {
            val expiry = LocalDateTime.parse(expiredTimeString)
            LocalDateTime.now().isAfter(expiry)
        } catch (e: DateTimeParseException) {
            // 파싱 오류가 발생하면 만료된 것으로 간주합니다.
            true
        }
    }

    /**
     * refreshToken 함수
     * DataStore에 저장된 refresh 토큰과 만료시간을 기반으로 새로운 access token 및 refresh token을 받아옵니다.
     * @return Boolean - 토큰 갱신 성공 여부
     */
    suspend fun refreshToken(): Boolean {
        val currentRefreshToken = dataStoreManager.refreshToken.first()
        val currentRefreshExpiry = dataStoreManager.refreshTokenExpiredTime.first()

        if (currentRefreshToken.isNullOrEmpty() || currentRefreshExpiry.isNullOrEmpty() || isAccessTokenExpired(
                currentRefreshExpiry
            )
        ) {
            return false
        }

        val response = memberRepository.refreshToken(currentRefreshToken)

        if (response.isSuccessful) {
            val loginResponse = response.body() ?: throw Exception("토큰 갱신 응답이 비어있습니다.")
            val tokenDto = loginResponse.data

            dataStoreManager.saveAccessToken(tokenDto.accessToken, tokenDto.accessTokenExpiredTime)
            dataStoreManager.saveRefreshToken(
                tokenDto.refreshToken,
                tokenDto.refreshTokenExpiredTime
            )

            return true
        }

        return false;
    }

    /**
     * 로그인 함수
     * 1. 우선 DataStore에 저장된 accessToken과 accessTokenExpiredTime을 확인합니다.
     * 2. 토큰이 존재하면 만료여부를 판단합니다.
     *    - 유효하면 바로 로그인 성공 처리합니다.
     *    - 만료되었다면 refreshToken() 함수를 통해 새 토큰 발급 후 새 토큰의 유효성을 확인합니다.
     * 3. 토큰이 없거나 갱신에 실패하면 기존 로그인 API 호출로 진행합니다.
     */
    fun login(loginDto: LoginDto) {
        Log.e("AuthViewModel", "login() 호출")
        viewModelScope.launch {
            _uiState.update { it.copy(loginState = AuthState.Loading) }
            try {
                val currentToken = dataStoreManager.accessToken.first()
                val currentTokenExpiry = dataStoreManager.accessTokenExpiredTime.first()

                if (currentToken != null && currentTokenExpiry != null) {
                    if (!isAccessTokenExpired(currentTokenExpiry)) {
                        _uiState.update { it.copy(loginState = AuthState.Success) }
                        return@launch
                    }

                    val refreshSuccess = refreshToken()
                    if (refreshSuccess) {
                        val newToken = dataStoreManager.accessToken.first()
                        val newTokenExpiry = dataStoreManager.accessTokenExpiredTime.first()

                        if (!newToken.isNullOrEmpty() && newTokenExpiry != null && !isAccessTokenExpired(newTokenExpiry)) {
                            _uiState.update { it.copy(loginState = AuthState.Success) }
                            return@launch
                        }
                    }
                }

                val response = memberRepository.login(loginDto)
                if (response.isSuccessful) {
                    val loginResponse = response.body() ?: throw Exception("로그인 응답이 비어있습니다.")
                    val tokenDto = loginResponse.data

                    dataStoreManager.saveAccessToken(
                        tokenDto.accessToken,
                        tokenDto.accessTokenExpiredTime
                    )
                    dataStoreManager.saveRefreshToken(
                        tokenDto.refreshToken,
                        tokenDto.refreshTokenExpiredTime
                    )

                    _uiState.update { it.copy(loginState = AuthState.Success) }
                } else {
                    throw Exception(response.errorBody()?.string() ?: "알 수 없는 에러")
                }
            } catch (e: Exception) {
                Log.e("login", e.message ?: "error")
                _uiState.update {
                    it.copy(loginState = AuthState.Error(e.message ?: UNKNOWN_ERROR))
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.update { it.copy(logoutState = AuthState.Loading) }
            try {
                dataStoreManager.clearTokens()
                _uiState.update {
                    it.copy(
                        logoutState = AuthState.Success,
                        hasToken = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        logoutState = AuthState.Error(e.message ?: UNKNOWN_ERROR)
                    )
                }
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
                token = null
            )
        }
    }
}
