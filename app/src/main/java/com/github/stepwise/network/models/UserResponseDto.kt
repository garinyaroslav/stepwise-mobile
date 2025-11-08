package com.github.stepwise.network.models

data class UserResponseDto(
    val id: Long?,
    val username: String?,
    val email: String?,
    val firstName: String?,
    val lastName: String?,
    val middleName: String?
)