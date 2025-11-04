package com.github.stepwise.network.models

data class WorkResponseDto(
    val id: Long,
    val title: String?,
    val description: String?,
    val countOfChapters: Int?,
    val type: String?,
    val teacherEmail: String?,
    val teacherFirstName: String?,
    val teacherLastName: String?,
    val teacherMiddleName: String?,
    val groupName: String?
)