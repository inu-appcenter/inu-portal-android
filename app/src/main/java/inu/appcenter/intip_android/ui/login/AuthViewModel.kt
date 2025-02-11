package inu.appcenter.intip_android.ui.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import inu.appcenter.intip_android.local.DataStoreManager
import inu.appcenter.intip_android.model.member.LoginDto
import inu.appcenter.intip_android.repository.member.MemberRepository
import inu.appcenter.intip_android.utils.K
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeParseException

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

    // 자동 토큰 갱신을 위한 Job 변수
    private var refreshJob: Job? = null

    init {
        observeToken()

        viewModelScope.launch {
            dataStoreManager.accessToken.collect { token ->
                Log.d("AuthViewModel", "Flow 업데이트, accessToken: $token")
            }
        }
    }

    /**
     * 토큰의 상태를 관찰하고, 새로운 토큰이 저장되면 자동 갱신 스케줄을 등록합니다.
     */
    private fun observeToken() {
        viewModelScope.launch {
            combine(
                dataStoreManager.accessToken,
                dataStoreManager.accessTokenExpiredTime
            ) { token, expiry -> Pair(token, expiry) }
                .distinctUntilChanged()  // 토큰 관련 값이 바뀔 때만 반응
                .collect { (token, expiry) ->
                    if (token != null && expiry != null) {
                        val valid = !isAccessTokenExpired(expiry)
                        _uiState.update { it.copy(hasToken = valid, token = token) }
                        if (valid) {
                            // 유효한 토큰이 있으면 자동 갱신 스케줄 등록
                            scheduleTokenRefresh(expiry)
                        } else {
                            // 만료된 경우 자동 갱신 작업 취소 (필요 시 추가 처리 가능)
                            refreshJob?.cancel()
                        }
                    } else {
                        _uiState.update { it.copy(hasToken = false, token = null) }
                        refreshJob?.cancel()
                    }
                }
        }
    }

    /**
     * access token 만료 1분 전(원하는 버퍼 시간) 자동 갱신을 위한 스케줄링 함수
     */
    private fun scheduleTokenRefresh(expiredTimeString: String) {
        refreshJob?.cancel() // 기존 스케줄 취소
        refreshJob = viewModelScope.launch {
            try {
                val expiry = LocalDateTime.parse(expiredTimeString)
                val now = LocalDateTime.now()
                // 만료 1분 전에 갱신 (원하는 버퍼 시간으로 조정 가능)
                val refreshTime = expiry.minusMinutes(1)
                val delayMillis = Duration.between(now, refreshTime).toMillis()
                if (delayMillis > 0) {
                    Log.d("AuthViewModel", "토큰 갱신까지 ${delayMillis}ms 대기")
                    delay(delayMillis)
                }
                Log.d("AuthViewModel", "자동 토큰 갱신 시도 (만료 1분 전)")
                val refreshed = refreshToken()
                if (refreshed) {
                    Log.d("AuthViewModel", "자동 토큰 갱신 성공")
                } else {
                    Log.e("AuthViewModel", "자동 토큰 갱신 실패")
                    // 자동 갱신 실패 시 로그아웃 처리하거나 추가 알림 로직 구현 가능
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "토큰 갱신 스케줄링 에러: ${e.message}")
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
     * 파라미터로 들어온 expiredTimeString (예: "2025-01-22T23:25:47.754524713")을 LocalDateTime으로 파싱하여 현재 시간과 비교합니다.
     */
    private fun isAccessTokenExpired(expiredTimeString: String): Boolean {
        return try {
            val expiry = LocalDateTime.parse(expiredTimeString)
            LocalDateTime.now().isAfter(expiry)
        } catch (e: DateTimeParseException) {
            // 파싱 오류 발생 시 만료된 것으로 간주
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
            dataStoreManager.saveRefreshToken(
                tokenDto.refreshToken,
                tokenDto.refreshTokenExpiredTime
            )

            return true
        }

        return false
    }

    /**
     * 로그인 함수
     * 1. DataStore에 저장된 accessToken과 accessTokenExpiredTime을 확인합니다.
     * 2. 토큰이 존재하면 만료 여부를 판단합니다.
     *    - 유효하면 바로 로그인 성공 처리합니다.
     *    - 만료되었다면 refreshToken() 함수를 통해 새 토큰 발급 시도 후, 성공하면 로그인 성공 처리합니다.
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
                val errorMessage = e.message?.let { extractErrorMessage(it) } ?: K.UNKNOWN_ERROR_MESSAGE
                _uiState.update {
                    it.copy(loginState = AuthState.Error(errorMessage))
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
                refreshJob?.cancel()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        logoutState = AuthState.Error(e.message ?: K.UNKNOWN_ERROR_MESSAGE)
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
                token = null,
                hasShownTokenAlert = false
            )
        }
    }

    /**
     * 서버 에러 메시지에서 "msg" 필드의 값만 추출하는 함수
     */
    private fun extractErrorMessage(message: String): String {
        val regex = """"msg"\s*:\s*"([^"]*)"""".toRegex()
        val matchResult = regex.find(message)
        return matchResult?.groupValues?.get(1) ?: message
    }
}
