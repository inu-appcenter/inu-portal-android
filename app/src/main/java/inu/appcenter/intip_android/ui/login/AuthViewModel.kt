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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val UNKNOWN_ERROR = "Unknown error"
private const val TIMEOUT_ERROR = "Signup timed out"

sealed class AuthState {
    data object Idle : AuthState()
    data object Loading : AuthState()
    data object Success : AuthState()
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
    val tokenFlow = dataStoreManager.token

    init {
        observeToken()
        viewModelScope.launch {
            tokenFlow.collect { token ->
                Log.d("AuthViewModel", "Current token in DataStore: $token")
            }
        }
    }

    private fun observeToken() {
        viewModelScope.launch {
            dataStoreManager.token
                .map { !it.isNullOrEmpty() }
                .collect { hasToken ->
                    _uiState.update { it.copy(hasToken = hasToken) }
                }
        }
    }

    fun setLoginId(loginId: String) {
        _uiState.update { it.copy(loginId = loginId) }
    }

    fun setLoginPw(loginPw: String) {
        _uiState.update { it.copy(loginPw = loginPw) }
    }

    fun saveToken(token: String) {
        viewModelScope.launch {
            try {
                dataStoreManager.saveToken(token)
                _uiState.update { it.copy(saveTokenState = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(saveTokenState = false) }
            }
        }
    }

    fun login(loginDto: LoginDto) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(loginState = AuthState.Loading) }
                val response = memberRepository.login(loginDto)
                if (response.isSuccessful) {
                    val loginResponse = response.body() ?: throw Exception("로그인 응답이 비어있습니다.")
                    // 성공한 경우 data에 TokenDto 객체가 존재
                    val tokenDto = loginResponse.data
                        ?: throw Exception(loginResponse.msg ?: "로그인 실패")

                    Log.d("AuthViewModel", "Received token: ${tokenDto.accessToken}")
                    dataStoreManager.saveToken(tokenDto.accessToken)
                    _uiState.update { it.copy(loginState = AuthState.Success) }
                } else {
                    throw Exception(response.errorBody()?.string() ?: "알 수 없는 에러")
                }
            } catch (e: Exception) {
                Log.e("login", e.message ?: "error")
                _uiState.update {
                    it.copy(loginState = AuthState.Error(e.message ?: "Unknown error"))
                }
            }
        }
    }


    fun logout() {
        viewModelScope.launch {
            _uiState.update { it.copy(logoutState = AuthState.Loading) }
            try {
                dataStoreManager.clearToken()

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
