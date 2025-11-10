package com.github.stepwise.network.models

import kotlinx.serialization.Serializable

@Serializable
data class ResetPasswordDto(
    val token: String,
    val newPassword: String
)