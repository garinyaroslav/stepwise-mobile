package com.github.stepwise.ui.student

import com.github.stepwise.network.models.ExplanatoryNoteItemResponseDto
import com.github.stepwise.network.models.ItemStatus
import com.github.stepwise.network.models.ProjectResponseDto
import com.github.stepwise.network.models.WorkChapterDto
import com.github.stepwise.network.models.WorkResponseDto
import org.junit.Assert.*
import org.junit.Test


class StudentProjectsFragmentTest {

    private fun computeProgressMap(
        works: List<WorkResponseDto>,
        projectsByWork: Map<Long, List<ProjectResponseDto>?>
    ): Map<Long, WorkProgressStats> {
        val progressMap = mutableMapOf<Long, WorkProgressStats>()

        works.mapNotNull { it.id }.forEach { workId ->
            val work = works.find { it.id == workId }

            val totalChapters = work?.countOfChapters
                ?: work?.academicWorkChapters?.size
                ?: 0

            val approvedCount = if (projectsByWork[workId] != null) {
                val projects = projectsByWork[workId] ?: emptyList()
                val project = projects.firstOrNull()
                project?.items?.count { it.status == ItemStatus.APPROVED } ?: 0
            } else 0

            progressMap[workId] = WorkProgressStats(approved = approvedCount, total = totalChapters)
        }

        return progressMap
    }

    @Test
    fun `computeProgressMap uses academicWorkChapters when countOfChapters is null`() {
        val work = WorkResponseDto(
            id = 1L,
            title = "Work A",
            description = null,
            countOfChapters = null,
            type = null,
            teacherEmail = null,
            teacherName = null,
            teacherLastName = null,
            teacherMiddleName = null,
            groupName = null,
            academicWorkChapters = listOf(
                WorkChapterDto(index = 1, title = "C1", description = null, deadline = null),
                WorkChapterDto(index = 2, title = "C2", description = null, deadline = null)
            )
        )

        val project = ProjectResponseDto(
            id = 10L,
            title = "Project",
            description = null,
            owner = null,
            items = listOf(
                ExplanatoryNoteItemResponseDto(id = 101, orderNumber = 1, status = ItemStatus.APPROVED, fileName = null, teacherComment = null, draftedAt = null, submittedAt = null, approvedAt = null, rejectedAt = null),
                ExplanatoryNoteItemResponseDto(id = 102, orderNumber = 2, status = ItemStatus.DRAFT, fileName = null, teacherComment = null, draftedAt = null, submittedAt = null, approvedAt = null, rejectedAt = null)
            ),
            isApprovedForDefense = false
        )

        val progress = computeProgressMap(listOf(work), mapOf(1L to listOf(project)))

        assertTrue(progress.containsKey(1L))
        val stats = progress[1L]!!
        assertEquals(1, stats.approved)
        assertEquals(2, stats.total)
    }

    @Test
    fun `computeProgressMap uses countOfChapters when present and zero approved on missing project response`() {
        val work = WorkResponseDto(
            id = 2L,
            title = "Work B",
            description = null,
            countOfChapters = 5,
            type = null,
            teacherEmail = null,
            teacherName = null,
            teacherLastName = null,
            teacherMiddleName = null,
            groupName = null,
            academicWorkChapters = null
        )

        val progress = computeProgressMap(listOf(work), mapOf(2L to null))

        assertTrue(progress.containsKey(2L))
        val stats = progress[2L]!!
        assertEquals(0, stats.approved)
        assertEquals(5, stats.total)
    }

    @Test
    fun `fragment instance creation is trivial and does not require Android runtime`() {
        val fragment = StudentProjectsFragment()
        assertNotNull(fragment)
    }
}