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
    val saveTokenState: Boolean? = null,
    val hasToken: Boolean? = null
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
                token != null && expiry != null && !isAccessTokenExpired(expiry)
            }.collect { valid ->
                _uiState.update { it.copy(hasToken = valid) }
                Log.d("AuthViewModel", "유효 토큰 여부: $valid")
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
        return try {
            val currentRefreshToken = dataStoreManager.refreshToken.first()
            val currentRefreshExpiry = dataStoreManager.refreshTokenExpiredTime.first()
            Log.d("AuthViewModel", "현재 refresh token: $currentRefreshToken, 만료시간: $currentRefreshExpiry")

            if (currentRefreshToken.isNullOrEmpty() || currentRefreshExpiry.isNullOrEmpty()) {
                throw Exception("저장된 refresh 토큰 정보가 없습니다.")
            }

            // refresh token 만료 여부 체크
            if (isAccessTokenExpired(currentRefreshExpiry)) {
                throw Exception("refresh 토큰이 만료되었습니다.")
            }

            // 토큰 갱신 API 호출
            val response = memberRepository.refreshToken(currentRefreshToken)
            Log.d("AuthViewModel", "refreshToken API 호출 성공, response: $response")

            if (response.isSuccessful) {
                val loginResponse = response.body()
                    ?: throw Exception("토큰 갱신 응답이 비어있습니다.")
                val tokenDto = loginResponse.data
                    ?: throw Exception(loginResponse.msg ?: "토큰 갱신 실패")

                Log.d("AuthViewModel", "Refresh 성공, 새 accessToken: ${tokenDto.accessToken}")
                Log.d("AuthViewModel", "Refresh 성공, 새 refreshToken: ${tokenDto.refreshToken}")

                // 새 토큰을 DataStore에 저장
                dataStoreManager.saveAccessToken(tokenDto.accessToken, tokenDto.accessTokenExpiredTime)
                dataStoreManager.saveRefreshToken(tokenDto.refreshToken, tokenDto.refreshTokenExpiredTime)

                // 저장 후 로그로 확인
                val latestToken = dataStoreManager.accessToken.first()
                val latestExpiry = dataStoreManager.accessTokenExpiredTime.first()
                Log.d("AuthViewModel", "새로 저장된 accessToken: $latestToken, 만료시간: $latestExpiry")

                val latestRefreshToken = dataStoreManager.refreshToken.first()
                val latestRefreshExpiry = dataStoreManager.refreshTokenExpiredTime.first()
                Log.d("AuthViewModel", "새로 저장된 refreshToken: $latestRefreshToken, 만료시간: $latestRefreshExpiry")

                true
            } else {
                Log.e("AuthViewModel", "refreshToken API 실패, 코드: ${response.code()}, 메시지: ${response.message()}")
                throw Exception(response.errorBody()?.string() ?: "토큰 갱신 에러")
            }
        } catch (e: Exception) {
            Log.e("refreshToken", e.message ?: "토큰 갱신 에러")
            false
        }
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
                // 저장된 accessToken과 만료시간을 가져옵니다.
                val currentToken = dataStoreManager.accessToken.first()
                val currentTokenExpiry = dataStoreManager.accessTokenExpiredTime.first()
                Log.d("AuthViewModel", "현재 token: $currentToken, 만료시간: $currentTokenExpiry")
                if (currentToken != null && currentTokenExpiry != null) {
                    if (!isAccessTokenExpired(currentTokenExpiry)) {
                        // 유효한 토큰이 존재하면 바로 로그인 성공 처리
                        Log.e("AuthViewModel", "저장된 유효 토큰 존재: $currentToken (만료시간: $currentTokenExpiry)")
                        _uiState.update { it.copy(loginState = AuthState.Success) }
                        return@launch
                    } else {
                        // 토큰이 만료된 경우, refreshToken() 호출을 통해 토큰 갱신 시도
                        Log.d("AuthViewModel", "만료된 토큰 발견 (만료시간: $currentTokenExpiry). 토큰 갱신 시도...")
                        val refreshSuccess = refreshToken()
                        if (refreshSuccess) {
                            // 새 토큰 정보 확인
                            val newToken = dataStoreManager.accessToken.first()
                            val newTokenExpiry = dataStoreManager.accessTokenExpiredTime.first()
                            Log.d("AuthViewModel", "토큰 갱신 후 토큰: $newToken, 만료시간: $newTokenExpiry")
                            if (!newToken.isNullOrEmpty() && newTokenExpiry != null && !isAccessTokenExpired(newTokenExpiry)) {
                                Log.d("AuthViewModel", "토큰 갱신 성공: $newToken (만료시간: $newTokenExpiry)")
                                _uiState.update { it.copy(loginState = AuthState.Success) }
                                return@launch
                            } else {
                                throw Exception("토큰 갱신에 실패했습니다.")
                            }
                        } else {
                            throw Exception("토큰 갱신에 실패했습니다.")
                        }
                    }
                }

                // 저장된 토큰이 없는 경우 기존 로그인 API 호출
                Log.d("AuthViewModel", "저장된 토큰이 없음. 로그인 API 호출")
                val response = memberRepository.login(loginDto)
                if (response.isSuccessful) {
                    val loginResponse = response.body()
                        ?: throw Exception("로그인 응답이 비어있습니다.")
                    // 로그인 성공 시 data에 TokenDto 객체가 포함되어 있음
                    val tokenDto = loginResponse.data
                        ?: throw Exception(loginResponse.msg ?: "로그인 실패")

                    Log.d("AuthViewModel", "로그인 API 응답 - access token: ${tokenDto.accessToken}")
                    Log.d("AuthViewModel", "로그인 API 응답 - refresh token: ${tokenDto.refreshToken}")

                    // 받아온 토큰과 만료시간을 DataStore에 저장합니다.
                    dataStoreManager.saveAccessToken(tokenDto.accessToken, tokenDto.accessTokenExpiredTime)
                    dataStoreManager.saveRefreshToken(tokenDto.refreshToken, tokenDto.refreshTokenExpiredTime)

                    // 저장 후 로그로 확인
                    val latestToken = dataStoreManager.accessToken.first()
                    val latestExpiry = dataStoreManager.accessTokenExpiredTime.first()
                    Log.d("AuthViewModel", "최신 저장 토큰: $latestToken, 만료시간: $latestExpiry")

                    val latestRefreshToken = dataStoreManager.refreshToken.first()
                    val latestRefreshExpiry = dataStoreManager.refreshTokenExpiredTime.first()
                    Log.d("AuthViewModel", "최신 저장 refreshToken: $latestRefreshToken, 만료시간: $latestRefreshExpiry")

                    _uiState.update { it.copy(loginState = AuthState.Success) }
                } else {
                    Log.e("AuthViewModel", "로그인 API 실패, 코드: ${response.code()}, 메시지: ${response.message()}")
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

    fun resetState(){
        _uiState.update {
            it.copy(
                loginState = AuthState.Idle,
                logoutState = AuthState.Idle,
                saveTokenState = null
            )
        }
    }
}
