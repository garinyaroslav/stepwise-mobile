package com.github.stepwise.network.models

data class ExplanatoryNoteItemResponseDto(
    val id: Long?,
    val orderNumber: Int?,
    val status: ItemStatus = ItemStatus.DRAFT,
    val fileName: String?,
    val teacherComment: String?,
    val draftedAt: String?,
    val submittedAt: String?,
    val approvedAt: String?,
    val rejectedAt: String?
)