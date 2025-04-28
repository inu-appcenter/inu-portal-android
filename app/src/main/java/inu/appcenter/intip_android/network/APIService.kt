package inu.appcenter.intip_android.network

import inu.appcenter.intip_android.model.fcm.FCMTokenUpdate
import inu.appcenter.intip_android.model.fcm.UpdateFCMTokenResponse
import inu.appcenter.intip_android.model.member.LoginDto
import inu.appcenter.intip_android.model.member.LoginResponse
import inu.appcenter.intip_android.utils.K
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface APIService {
    @POST(K.LOGIN)
    suspend fun login(
        @Body loginDto: LoginDto
    ) : Response<LoginResponse>

    @POST(K.REFRESH)
    suspend fun refreshToken(
        @Header("refresh") refreshToken: String
    ): Response<LoginResponse>

    @POST(K.FCM_TOKEN_UPDATE)
    suspend fun updateFCMToken(
        @Body body: FCMTokenUpdate
    ) : Response<UpdateFCMTokenResponse>
}