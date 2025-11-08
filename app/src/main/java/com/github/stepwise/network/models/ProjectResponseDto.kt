package com.github.stepwise.network.models

data class ProjectResponseDto(
    val id: Long?,
    val title: String?,
    val description: String?,
    val owner: UserResponseDto?,
    val items: List<ExplanatoryNoteItemResponseDto>?,
    val isApprovedForDefense: Boolean = false
)