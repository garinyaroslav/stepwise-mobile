package com.github.stepwise.network.models

import com.google.gson.annotations.SerializedName

data class ProjectResponseDto(
    val id: Long?,
    val title: String?,
    val description: String?,
    val owner: UserResponseDto?,
    val items: List<ExplanatoryNoteItemResponseDto>?,

    @SerializedName(
        value = "isApprovedForDefense",
        alternate = ["approvedForDefense"]
    )
    val isApprovedForDefense: Boolean = false
)