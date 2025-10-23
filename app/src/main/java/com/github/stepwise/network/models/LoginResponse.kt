package com.github.stepwise.network.models

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("user")
    val user: User? = null,

    @SerializedName("token")
    val token: String? = null
) {
    val role: String?
        get() = user?.role
}

data class User(
    @SerializedName("role")
    val role: String? = null
)