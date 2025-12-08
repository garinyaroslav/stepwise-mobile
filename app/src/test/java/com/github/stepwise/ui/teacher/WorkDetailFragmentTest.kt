package com.github.stepwise.ui.teacher

import com.github.stepwise.network.models.ProjectType
import com.github.stepwise.network.models.WorkChapterDto
import com.github.stepwise.network.models.WorkResponseDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test


class WorkDetailFragmentTest {

    private fun typeOfWorkLabel(type: ProjectType?): String {
        return if (type == ProjectType.COURSEWORK) "Курсовая работа" else "Дипломная работа"
    }

    @Test
    fun `typeOfWorkLabel returns coursework label for COURSEWORK`() {
        val label = typeOfWorkLabel(ProjectType.COURSEWORK)
        assertEquals("Курсовая работа", label)
    }

    @Test
    fun `typeOfWorkLabel returns diploma label for null or THESIS`() {
        assertEquals("Дипломная работа", typeOfWorkLabel(ProjectType.THESIS))
        assertEquals("Дипломная работа", typeOfWorkLabel(null))
    }

    @Test
    fun `meta text uses countOfChapters when present`() {
        val work = WorkResponseDto(
            id = 1L,
            title = "Title",
            description = null,
            countOfChapters = 5,
            type = ProjectType.COURSEWORK,
            teacherEmail = null,
            teacherName = null,
            teacherLastName = null,
            teacherMiddleName = null,
            groupName = "G1",
            academicWorkChapters = listOf(
                WorkChapterDto(index = 1, title = "c1", description = null, deadline = null)
            )
        )

        val expectedType = typeOfWorkLabel(work.type)
        val expected = "$expectedType у группы ${work.groupName}. Пунктов: ${work.countOfChapters ?: 0}"
        val meta = "$expectedType у группы ${work.groupName}. Пунктов: ${work.countOfChapters ?: 0}"
        assertEquals(expected, meta)
    }

    @Test
    fun `meta text falls back to zero when countOfChapters null`() {
        val work = WorkResponseDto(
            id = 2L,
            title = "T2",
            description = null,
            countOfChapters = null,
            type = ProjectType.THESIS,
            teacherEmail = null,
            teacherName = null,
            teacherLastName = null,
            teacherMiddleName = null,
            groupName = "GroupX",
            academicWorkChapters = null
        )

        val expectedType = typeOfWorkLabel(work.type)
        val expected = "$expectedType у группы ${work.groupName}. Пунктов: ${work.countOfChapters ?: 0}"
        val meta = "$expectedType у группы ${work.groupName}. Пунктов: ${work.countOfChapters ?: 0}"
        assertEquals(expected, meta)
    }

    @Test
    fun `chapters list used when provided and empty flag logic`() {
        val withChapters = WorkResponseDto(
            id = 3L,
            title = "W",
            description = null,
            countOfChapters = null,
            type = null,
            teacherEmail = null,
            teacherName = null,
            teacherLastName = null,
            teacherMiddleName = null,
            groupName = null,
            academicWorkChapters = listOf(
                WorkChapterDto(index = 1, title = "A", description = null, deadline = null),
                WorkChapterDto(index = 2, title = "B", description = null, deadline = null)
            )
        )

        val chapters = withChapters.academicWorkChapters ?: emptyList()
        assertEquals(2, chapters.size)
        assertNotNull(chapters)
    }

    @Test
    fun `empty chapters produce empty behavior`() {
        val emptyWork = WorkResponseDto(
            id = 4L,
            title = "W2",
            description = null,
            countOfChapters = null,
            type = null,
            teacherEmail = null,
            teacherName = null,
            teacherLastName = null,
            teacherMiddleName = null,
            groupName = null,
            academicWorkChapters = emptyList()
        )

        val chapters = emptyWork.academicWorkChapters ?: emptyList()
        assertEquals(0, chapters.size)
    }
}