package inu.appcenter.intip_android.repository.member

import inu.appcenter.intip_android.model.member.LoginDto
import inu.appcenter.intip_android.model.member.LoginResponse
import inu.appcenter.intip_android.model.member.TokenDto
import inu.appcenter.intip_android.network.APIService
import retrofit2.Response

class MemberRepositoryImpl(private val apiService: APIService) : MemberRepository {
    override suspend fun login(loginDto: LoginDto): Response<LoginResponse> {
        return apiService.login(loginDto = loginDto)
    }

    override suspend fun refreshToken(refreshToken: String): Response<LoginResponse> {
        return apiService.refreshToken("Bearer $refreshToken")
    }
}