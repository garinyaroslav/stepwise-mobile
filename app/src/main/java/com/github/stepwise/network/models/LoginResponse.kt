package com.github.stepwise.network.models

import kotlinx.serialization.Serializable

@Serializable
data class UserShort(
    val id: Long?,
    val role: String?
)

@Serializable
data class LoginResponse(
    val user: UserShort? = null,
    val token: String? = null,
    val role: String? = null,
    val temporaryPassword: Boolean? = null
)