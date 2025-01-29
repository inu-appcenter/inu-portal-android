package inu.appcenter.intip_android.model.member

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("data")
    val data: TokenDto, // 성공 시 token 정보가 들어 있음
    @SerializedName("msg")
    val msg: String?
)