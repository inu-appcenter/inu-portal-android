package inu.appcenter.intip_android.model.member

import com.google.gson.annotations.SerializedName

data class ResponseDto(
    @SerializedName("data") val data: Any?,
    @SerializedName("message") val message: String
)
