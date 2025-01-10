package inu.appcenter.intip_android.repository.member

import inu.appcenter.intip_android.model.member.LoginDto
import inu.appcenter.intip_android.model.member.TokenDto
import retrofit2.Response

interface MemberRepository {
    suspend fun login(loginDto: LoginDto): Response<TokenDto>
}