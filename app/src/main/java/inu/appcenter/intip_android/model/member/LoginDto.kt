package inu.appcenter.intip_android.model.member

import com.google.gson.annotations.SerializedName

data class LoginDto(
    @SerializedName("studentId") val studentId: String,
    @SerializedName("password") val password: String,
)
