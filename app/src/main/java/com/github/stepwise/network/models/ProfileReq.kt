package com.github.stepwise.network.models

data class ProfileReq(
    val id: Long? = null,
    val firstName: String?,
    val lastName: String?,
    val middleName: String? = null,
    val phoneNumber: String?,
    val address: String? = null
)