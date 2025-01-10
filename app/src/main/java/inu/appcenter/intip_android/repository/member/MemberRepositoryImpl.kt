package inu.appcenter.intip_android.repository.member

import inu.appcenter.intip_android.model.member.LoginDto
import inu.appcenter.intip_android.model.member.TokenDto
import inu.appcenter.intip_android.network.APIService
import retrofit2.Response

class MemberRepositoryImpl(private val apiService: APIService) : MemberRepository {
    override suspend fun login(loginDto: LoginDto): Response<TokenDto> {
        return apiService.login(loginDto = loginDto)
    }
}