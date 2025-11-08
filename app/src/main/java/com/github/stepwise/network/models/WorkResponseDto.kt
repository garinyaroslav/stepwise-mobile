package com.github.stepwise.network.models

data class WorkResponseDto(
    val id: Long?,
    val title: String?,
    val description: String?,
    val countOfChapters: Int?,
    val type: ProjectType?,
    val teacherEmail: String?,
    val teacherName: String?,
    val teacherLastName: String?,
    val teacherMiddleName: String?,
    val groupName: String?,
    val academicWorkChapters: List<WorkChapterDto>? = null
)