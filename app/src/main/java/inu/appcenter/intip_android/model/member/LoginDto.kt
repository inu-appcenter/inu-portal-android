package inu.appcenter.intip_android.model.member

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class LoginDto(
    @SerializedName("studentId") val studentId: String,
    @SerializedName("password") val password: String,
)
