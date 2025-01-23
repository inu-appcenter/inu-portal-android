package inu.appcenter.intip_android.network

import inu.appcenter.intip_android.model.member.LoginDto
import inu.appcenter.intip_android.model.member.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface APIService {
    @POST("/api/members/login")
    suspend fun login(
        @Body loginDto: LoginDto
    ) : Response<LoginResponse>

    @POST("/api/members/refresh")
    suspend fun refreshToken(
        @Header("refresh") refreshToken: String
    ): Response<LoginResponse>
}
