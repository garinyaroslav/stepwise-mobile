package com.github.stepwise.ui.work

import com.github.stepwise.network.models.WorkChapterReq
import org.junit.Assert.assertEquals
import org.junit.Test

class ChaptersAdapterTest {

    private fun mapStatesToReqs(states: List<ChaptersAdapter.ChapterState>): List<WorkChapterReq> {
        return states.mapIndexed { index, s ->
            WorkChapterReq(
                index = index + 1,
                title = s.title.ifBlank { null },
                description = s.description.ifBlank { null },
                deadline = s.deadlineIso
            )
        }
    }

    @Test
    fun `mapping preserves index and converts blanks to nulls`() {
        val states = listOf(
            ChaptersAdapter.ChapterState(title = "First", description = "Desc1", deadlineIso = "2025-12-08T23:59:59"),
            ChaptersAdapter.ChapterState(title = "", description = "   ", deadlineIso = null),
            ChaptersAdapter.ChapterState(title = "Third", description = "", deadlineIso = "2025-12-09T23:59:59")
        )

        val reqs = mapStatesToReqs(states)

        assertEquals(3, reqs.size)

        val r1 = reqs[0]
        assertEquals(1, r1.index)
        assertEquals("First", r1.title)
        assertEquals("Desc1", r1.description)
        assertEquals("2025-12-08T23:59:59", r1.deadline)

        val r2 = reqs[1]
        assertEquals(2, r2.index)
        assertEquals(null, r2.title)
        assertEquals(null, r2.description)
        assertEquals(null, r2.deadline)

        val r3 = reqs[2]
        assertEquals(3, r3.index)
        assertEquals("Third", r3.title)
        assertEquals(null, r3.description)
        assertEquals("2025-12-09T23:59:59", r3.deadline)
    }
}