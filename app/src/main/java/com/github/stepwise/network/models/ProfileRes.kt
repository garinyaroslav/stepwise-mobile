package com.github.stepwise.network.models

data class ProfileRes(
    val id: Long?,
    val username: String?,
    val email: String?,
    val firstName: String?,
    val lastName: String?,
    val middleName: String?,
    val phoneNumber: String?,
    val address: String?
)