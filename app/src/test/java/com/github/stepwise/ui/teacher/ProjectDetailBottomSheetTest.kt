package com.github.stepwise.ui.teacher

import com.github.stepwise.network.models.ExplanatoryNoteItemResponseDto
import com.github.stepwise.network.models.ItemStatus
import org.junit.Assert.*
import org.junit.Test

class ProjectDetailBottomSheetTest {

    @Test
    fun `canShowApproveProject true when all conditions met`() {
        val chapterCount = 3
        val items = listOf(
            ExplanatoryNoteItemResponseDto(id = 1, orderNumber = 1, status = ItemStatus.APPROVED, fileName = null, teacherComment = null, draftedAt = null, submittedAt = null, approvedAt = null, rejectedAt = null),
            ExplanatoryNoteItemResponseDto(id = 2, orderNumber = 2, status = ItemStatus.APPROVED, fileName = null, teacherComment = null, draftedAt = null, submittedAt = null, approvedAt = null, rejectedAt = null),
            ExplanatoryNoteItemResponseDto(id = 3, orderNumber = 3, status = ItemStatus.APPROVED, fileName = null, teacherComment = null, draftedAt = null, submittedAt = null, approvedAt = null, rejectedAt = null)
        )

        val attachedCount = items.mapNotNull { it.orderNumber }.toSet().size
        val allApproved = items.isNotEmpty() && items.all { it.status == ItemStatus.APPROVED }
        val allChaptersAttached = chapterCount > 0 && attachedCount >= chapterCount
        val isApprovedForDefense = false

        val canShow = !isApprovedForDefense && allApproved && allChaptersAttached
        assertTrue(canShow)
    }

    @Test
    fun `canShowApproveProject false when not all approved`() {
        val chapterCount = 2
        val items = listOf(
            ExplanatoryNoteItemResponseDto(id = 1, orderNumber = 1, status = ItemStatus.APPROVED, fileName = null, teacherComment = null, draftedAt = null, submittedAt = null, approvedAt = null, rejectedAt = null),
            ExplanatoryNoteItemResponseDto(id = 2, orderNumber = 2, status = ItemStatus.DRAFT, fileName = null, teacherComment = null, draftedAt = null, submittedAt = null, approvedAt = null, rejectedAt = null)
        )

        val attachedCount = items.mapNotNull { it.orderNumber }.toSet().size
        val allApproved = items.isNotEmpty() && items.all { it.status == ItemStatus.APPROVED }
        val allChaptersAttached = chapterCount > 0 && attachedCount >= chapterCount
        val isApprovedForDefense = false

        val canShow = !isApprovedForDefense && allApproved && allChaptersAttached
        assertFalse(canShow)
    }

    @Test
    fun `canShowApproveProject false when not all chapters attached`() {
        val chapterCount = 4
        val items = listOf(
            ExplanatoryNoteItemResponseDto(id = 1, orderNumber = 1, status = ItemStatus.APPROVED, fileName = null, teacherComment = null, draftedAt = null, submittedAt = null, approvedAt = null, rejectedAt = null),
            ExplanatoryNoteItemResponseDto(id = 2, orderNumber = 2, status = ItemStatus.APPROVED, fileName = null, teacherComment = null, draftedAt = null, submittedAt = null, approvedAt = null, rejectedAt = null)
        )

        val attachedCount = items.mapNotNull { it.orderNumber }.toSet().size
        val allApproved = items.isNotEmpty() && items.all { it.status == ItemStatus.APPROVED }
        val allChaptersAttached = chapterCount > 0 && attachedCount >= chapterCount
        val isApprovedForDefense = false

        val canShow = !isApprovedForDefense && allApproved && allChaptersAttached
        assertFalse(canShow)
    }
}