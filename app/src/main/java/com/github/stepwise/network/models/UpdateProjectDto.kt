package com.github.stepwise.network.models

data class UpdateProjectDto(
    val id: Long,
    val title: String,
    val description: String
)