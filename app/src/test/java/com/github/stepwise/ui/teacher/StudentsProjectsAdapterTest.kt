package com.github.stepwise.ui.teacher

import com.github.stepwise.network.models.ExplanatoryNoteItemResponseDto
import com.github.stepwise.network.models.ItemStatus
import com.github.stepwise.network.models.UserResponseDto
import org.junit.Assert.*
import org.junit.Test

class StudentsProjectsAdapterTest {

    private fun pluralize(count: Int): String {
        val companionClass = StudentsProjectsAdapter::class.java.declaredClasses.first { it.simpleName == "Companion" }
        val companionField = StudentsProjectsAdapter::class.java.getDeclaredField("Companion")
        companionField.isAccessible = true
        val companionInstance = companionField.get(null)
        val method = companionClass.getDeclaredMethod("pluralizePunkt", Int::class.javaPrimitiveType)
        method.isAccessible = true
        return method.invoke(companionInstance, count) as String
    }

    @Test
    fun `pluralizePunkt returns correct form for various numbers`() {
        assertEquals("пунктов", pluralize(0))
        assertEquals("пункт", pluralize(1))
        assertEquals("пункта", pluralize(2))
        assertEquals("пункта", pluralize(4))
        assertEquals("пунктов", pluralize(5))
        assertEquals("пунктов", pluralize(11))
        assertEquals("пунктов", pluralize(14))
        assertEquals("пункт", pluralize(21))
        assertEquals("пункта", pluralize(22))
        assertEquals("пунктов", pluralize(111))
    }

    @Test
    fun `last item selection logic chooses max by submittedAt or draftedAt`() {
        val a = ExplanatoryNoteItemResponseDto(
            id = 1L, orderNumber = 1, status = ItemStatus.DRAFT,
            fileName = null, teacherComment = null,
            draftedAt = "2020-01-01T00:00:00", submittedAt = null, approvedAt = null, rejectedAt = null
        )
        val b = ExplanatoryNoteItemResponseDto(
            id = 2L, orderNumber = 2, status = ItemStatus.DRAFT,
            fileName = null, teacherComment = null,
            draftedAt = "2020-02-01T00:00:00", submittedAt = "2020-02-01T00:00:00", approvedAt = null, rejectedAt = null
        )
        val c = ExplanatoryNoteItemResponseDto(
            id = 3L, orderNumber = 3, status = ItemStatus.DRAFT,
            fileName = null, teacherComment = null,
            draftedAt = "2020-03-01T00:00:00", submittedAt = null, approvedAt = null, rejectedAt = null
        )

        val list = listOf(a, b, c)
        val last = list.maxByOrNull { it.submittedAt ?: it.draftedAt ?: "" }
        assertNotNull(last)
        assertEquals(3L, last!!.id)
    }

    @Test
    fun `owner display logic - prefer names, fallback to email or Student`() {
        fun ownerDisplay(owner: UserResponseDto?): String {
            return when {
                owner != null -> listOfNotNull(owner.firstName, owner.lastName).joinToString(" ").ifBlank {
                    owner.email ?: "Студент"
                }
                else -> "Студент"
            }
        }

        val ownerNames = UserResponseDto(id = 1L, username = null, email = "e@example.com", firstName = "Ivan", lastName = "Petrov", middleName = null)
        assertEquals("Ivan Petrov", ownerDisplay(ownerNames))

        val ownerBlankNames = UserResponseDto(id = 2L, username = null, email = "bob@example.com", firstName = "", lastName = "", middleName = null)
        assertEquals("bob@example.com", ownerDisplay(ownerBlankNames))

        val ownerNoEmailNoNames = UserResponseDto(id = 3L, username = null, email = null, firstName = "", lastName = "", middleName = null)
        assertEquals("Студент", ownerDisplay(ownerNoEmailNoNames))

        val nullOwner: UserResponseDto? = null
        assertEquals("Студент", ownerDisplay(nullOwner))
    }
}