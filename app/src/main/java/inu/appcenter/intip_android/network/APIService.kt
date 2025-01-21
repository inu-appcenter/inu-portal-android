package inu.appcenter.intip_android.network

import inu.appcenter.intip_android.model.member.LoginDto
import inu.appcenter.intip_android.model.member.LoginResponse
import inu.appcenter.intip_android.model.member.TokenDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface APIService {
    // Members API
    @POST("/api/members/login")
    suspend fun login(
        @Body loginDto: LoginDto
    ) : Response<LoginResponse>
}