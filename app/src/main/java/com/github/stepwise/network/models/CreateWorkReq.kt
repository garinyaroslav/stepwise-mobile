package com.github.stepwise.network.models

data class CreateWorkReq(
    val title: String?,
    val description: String?,
    val type: ProjectType?,
    val groupId: Long?,
    val teacherId: Long?,
    val chapters: List<WorkChapterReq> = emptyList()
)

data class WorkChapterReq(
    val index: Int,
    val title: String?,
    val description: String?,
    val deadline: String?
)