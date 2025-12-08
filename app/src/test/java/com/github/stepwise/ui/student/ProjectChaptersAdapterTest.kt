package com.github.stepwise.ui.student

import com.github.stepwise.network.models.ExplanatoryNoteItemResponseDto
import com.github.stepwise.network.models.ItemStatus
import com.github.stepwise.network.models.WorkChapterDto
import org.junit.Assert.*
import org.junit.Test

class ProjectChaptersAdapterTest {

    private fun computeFirstAttachablePosition(list: List<Pair<WorkChapterDto, ExplanatoryNoteItemResponseDto?>>?): Int? {
        return list
            ?.withIndex()
            ?.firstOrNull { (_, pair) ->
                val item = pair.second
                item == null || item.status.name == "DRAFT" || item.status.name == "REJECTED"
            }?.index
    }

    @Test
    fun `firstAttachablePosition is null for empty or null list`() {
        assertNull(computeFirstAttachablePosition(null))
        assertNull(computeFirstAttachablePosition(emptyList()))
    }

    @Test
    fun `firstAttachablePosition picks first null item`() {
        val list = listOf(
            Pair(WorkChapterDto(1, "c1", null, null), null),
            Pair(WorkChapterDto(2, "c2", null, null), ExplanatoryNoteItemResponseDto(2, 2, ItemStatus.APPROVED, "file.pdf", null, null, null, null, null))
        )
        val idx = computeFirstAttachablePosition(list)
        assertEquals(0, idx)
    }

    @Test
    fun `firstAttachablePosition skips approved and picks draft or rejected`() {
        val list = listOf(
            Pair(WorkChapterDto(1, "c1", null, null), ExplanatoryNoteItemResponseDto(1, 1, ItemStatus.APPROVED, "a.pdf", null, null, null, null, null)),
            Pair(WorkChapterDto(2, "c2", null, null), ExplanatoryNoteItemResponseDto(2, 2, ItemStatus.SUBMITTED, "b.pdf", null, null, null, null, null)),
            Pair(WorkChapterDto(3, "c3", null, null), ExplanatoryNoteItemResponseDto(3, 3, ItemStatus.DRAFT, null, null, null, null, null, null)),
            Pair(WorkChapterDto(4, "c4", null, null), ExplanatoryNoteItemResponseDto(4, 4, ItemStatus.REJECTED, "c.pdf", "Please fix", null, null, null, null))
        )
        val idx = computeFirstAttachablePosition(list)
        assertEquals(2, idx)
    }

    @Test
    fun `hasFile detection works as expected`() {
        val withFile = ExplanatoryNoteItemResponseDto(1, 1, ItemStatus.DRAFT, "file.pdf", null, null, null, null, null)
        val withEmpty = ExplanatoryNoteItemResponseDto(2, 2, ItemStatus.DRAFT, "", null, null, null, null, null)
        val withNull = ExplanatoryNoteItemResponseDto(3, 3, ItemStatus.DRAFT, null, null, null, null, null, null)

        fun hasFile(item: ExplanatoryNoteItemResponseDto?): Boolean = item?.fileName?.isNotBlank() == true

        assertTrue(hasFile(withFile))
        assertFalse(hasFile(withEmpty))
        assertFalse(hasFile(withNull))
        assertFalse(hasFile(null))
    }

    @Test
    fun `status text fallback and russian names`() {
        val nullItem: ExplanatoryNoteItemResponseDto? = null
        val statusTextNull = nullItem?.status?.russian() ?: "Не прикреплён"
        assertEquals("Не прикреплён", statusTextNull)

        assertEquals("Черновик", ItemStatus.DRAFT.russian())
        assertEquals("Отправлен", ItemStatus.SUBMITTED.russian())
        assertEquals("Одобрено", ItemStatus.APPROVED.russian())
        assertEquals("Отклонено", ItemStatus.REJECTED.russian())
    }
}