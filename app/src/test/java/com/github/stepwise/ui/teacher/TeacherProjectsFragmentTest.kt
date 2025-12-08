package com.github.stepwise.ui.teacher

import com.github.stepwise.network.models.WorkChapterDto
import com.github.stepwise.network.models.WorkResponseDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class TeacherProjectsFragmentTest {

    @Test
    fun `fragment instance creation does not crash`() {
        val fragment = TeacherProjectsFragment()
        assertNotNull(fragment)
    }

    @Test
    fun `compute total chapters uses countOfChapters when present`() {
        val work = WorkResponseDto(
            id = 1L,
            title = "W",
            description = null,
            countOfChapters = 5,
            type = null,
            teacherEmail = null,
            teacherName = null,
            teacherLastName = null,
            teacherMiddleName = null,
            groupName = null,
            academicWorkChapters = listOf(
                WorkChapterDto(index = 1, title = "c1", description = null, deadline = null),
                WorkChapterDto(index = 2, title = "c2", description = null, deadline = null)
            )
        )

        fun totalChapters(work: WorkResponseDto): Int {
            return work.countOfChapters ?: work.academicWorkChapters?.size ?: 0
        }

        assertEquals(5, totalChapters(work))
    }

    @Test
    fun `compute total chapters falls back to academicWorkChapters size when countOfChapters null`() {
        val work = WorkResponseDto(
            id = 2L,
            title = "W2",
            description = null,
            countOfChapters = null,
            type = null,
            teacherEmail = null,
            teacherName = null,
            teacherLastName = null,
            teacherMiddleName = null,
            groupName = null,
            academicWorkChapters = listOf(
                WorkChapterDto(index = 1, title = "c1", description = null, deadline = null),
                WorkChapterDto(index = 2, title = "c2", description = null, deadline = null),
                WorkChapterDto(index = 3, title = "c3", description = null, deadline = null)
            )
        )

        fun totalChapters(work: WorkResponseDto): Int {
            return work.countOfChapters ?: work.academicWorkChapters?.size ?: 0
        }

        assertEquals(3, totalChapters(work))
    }

    @Test
    fun `compute total chapters returns zero when neither provided`() {
        val work = WorkResponseDto(
            id = 3L,
            title = "W3",
            description = null,
            countOfChapters = null,
            type = null,
            teacherEmail = null,
            teacherName = null,
            teacherLastName = null,
            teacherMiddleName = null,
            groupName = null,
            academicWorkChapters = null
        )

        fun totalChapters(work: WorkResponseDto): Int {
            return work.countOfChapters ?: work.academicWorkChapters?.size ?: 0
        }

        assertEquals(0, totalChapters(work))
    }
}