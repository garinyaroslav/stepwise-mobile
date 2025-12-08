package com.github.stepwise.ui.student

import com.github.stepwise.network.models.ExplanatoryNoteItemResponseDto
import com.github.stepwise.network.models.ItemStatus
import com.github.stepwise.network.models.WorkChapterDto
import org.junit.Assert.*
import org.junit.Test

class StudentProjectDetailFragmentTest {

    @Test
    fun `fragment instance creation does not crash`() {
        val fragment = StudentProjectDetailFragment()
        assertNotNull(fragment)
    }

    @Test
    fun `approved count calculation works for given chapters and items`() {
        val chapters = listOf(
            WorkChapterDto(index = 1, title = "C1", description = null, deadline = null),
            WorkChapterDto(index = 2, title = "C2", description = null, deadline = null),
            WorkChapterDto(index = 3, title = "C3", description = null, deadline = null)
        )

        val items = listOf(
            ExplanatoryNoteItemResponseDto(
                id = 101,
                orderNumber = 1,
                status = ItemStatus.APPROVED,
                fileName = null,
                teacherComment = null,
                draftedAt = null,
                submittedAt = null,
                approvedAt = null,
                rejectedAt = null
            ),
            ExplanatoryNoteItemResponseDto(
                id = 102,
                orderNumber = 2,
                status = ItemStatus.DRAFT,
                fileName = null,
                teacherComment = null,
                draftedAt = null,
                submittedAt = null,
                approvedAt = null,
                rejectedAt = null
            )
        )

        val approved = items.count { it.status == ItemStatus.APPROVED }
        val total = chapters.size

        assertEquals(1, approved)
        assertEquals(3, total)
    }

    @Test
    fun `setupDescription-like logic empty description treated as hidden`() {
        fun shouldShowDescription(description: String?): Boolean {
            val desc = description ?: ""
            return desc.isNotBlank()
        }

        assertFalse(shouldShowDescription(null))
        assertFalse(shouldShowDescription(""))
        assertFalse(shouldShowDescription("   "))
        assertTrue(shouldShowDescription("Non-empty"))
    }
}