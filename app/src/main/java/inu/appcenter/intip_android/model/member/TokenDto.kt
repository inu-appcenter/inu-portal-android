package inu.appcenter.intip_android.model.member

import com.google.gson.annotations.SerializedName

data class TokenDto(
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("accessTokenExpiredTime") val accessTokenExpiredTime: String,
    @SerializedName("refreshToken") val refreshToken: String,
    @SerializedName("refreshTokenExpiredTime") val refreshTokenExpiredTime: String
)
