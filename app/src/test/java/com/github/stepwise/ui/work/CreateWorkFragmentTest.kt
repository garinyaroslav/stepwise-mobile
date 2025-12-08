package com.github.stepwise.ui.work

import com.github.stepwise.network.models.ProjectType
import org.junit.Assert.*
import org.junit.Test


class CreateWorkFragmentTest {

    private fun getTypeDisplayAndValues(): Pair<List<String>, List<ProjectType>> {

        val display = listOf("Курсовая", "Дипломная работа")
        val values = listOf(ProjectType.COURSEWORK, ProjectType.THESIS)
        return display to values
    }

    private fun validateSubmit(
        title: String,
        groupTag: Any?,
        chapters: List<ChaptersAdapter.ChapterState>
    ): String? {
        val trimmedTitle = title.trim()
        if (groupTag !is Long) return "group"

        if (trimmedTitle.length < 3) return "title"

        if (chapters.isEmpty()) return "Добавьте хотя бы одну часть"

        for ((index, chapter) in chapters.withIndex()) {
            if (chapter.title.isBlank()) return "Часть ${index + 1}: укажите название"
            if (chapter.deadlineIso.isNullOrBlank()) return "Часть ${index + 1}: укажите дедлайн"
        }
        return null
    }

    @Test
    fun `typeDisplay maps to typeValues expected`() {
        val (display, values) = getTypeDisplayAndValues()
        assertEquals(2, display.size)
        assertEquals(2, values.size)
        assertEquals("Курсовая", display[0])
        assertEquals(ProjectType.COURSEWORK, values[0])
        assertEquals("Дипломная работа", display[1])
        assertEquals(ProjectType.THESIS, values[1])
    }

    @Test
    fun `validation fails when group not selected`() {
        val chapters = listOf(
            ChaptersAdapter.ChapterState(title = "Part 1", description = "d", deadlineIso = "2025-12-08T23:59:59")
        )
        val err = validateSubmit(title = "My work", groupTag = null, chapters = chapters)
        assertEquals("group", err)
    }

    @Test
    fun `validation fails when title too short`() {
        val chapters = listOf(
            ChaptersAdapter.ChapterState(title = "P1", description = "d", deadlineIso = "2025-12-08T23:59:59")
        )
        val err = validateSubmit(title = "ab", groupTag = 123L, chapters = chapters)
        assertEquals("title", err)
    }

    @Test
    fun `validation fails when no chapters`() {
        val err = validateSubmit(title = "Valid title", groupTag = 5L, chapters = emptyList())
        assertEquals("Добавьте хотя бы одну часть", err)
    }

    @Test
    fun `validation fails when chapter title blank`() {
        val chapters = listOf(
            ChaptersAdapter.ChapterState(title = "", description = "d", deadlineIso = "2025-12-08T23:59:59")
        )
        val err = validateSubmit(title = "Valid title", groupTag = 5L, chapters = chapters)
        assertEquals("Часть 1: укажите название", err)
    }

    @Test
    fun `validation fails when chapter deadline missing`() {
        val chapters = listOf(
            ChaptersAdapter.ChapterState(title = "Chapter 1", description = "d", deadlineIso = null)
        )
        val err = validateSubmit(title = "Valid title", groupTag = 5L, chapters = chapters)
        assertEquals("Часть 1: укажите дедлайн", err)
    }

    @Test
    fun `validation succeeds for valid input`() {
        val chapters = listOf(
            ChaptersAdapter.ChapterState(title = "Chapter 1", description = "d", deadlineIso = "2025-12-08T23:59:59"),
            ChaptersAdapter.ChapterState(title = "Chapter 2", description = "d2", deadlineIso = "2025-12-09T23:59:59")
        )
        val err = validateSubmit(title = "Valid title", groupTag = 42L, chapters = chapters)
        assertNull(err)
    }
}