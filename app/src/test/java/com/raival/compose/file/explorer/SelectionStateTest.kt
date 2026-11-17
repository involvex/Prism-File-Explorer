package com.raival.compose.file.explorer

import com.raival.compose.file.explorer.common.removeIf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectionStateTest {

    @Test
    fun removeIfRemovesOnlyMatchingKeys() {
        val selection = linkedMapOf(
            "/a" to "a",
            "/b" to "b",
            "/c" to "c"
        )

        selection.removeIf { key, value -> key == "/b" || value == "c" }

        assertEquals(mapOf("/a" to "a"), selection)
    }

    @Test
    fun removeIfWorksOnEmptyMap() {
        val selection = linkedMapOf<String, String>()
        selection.removeIf { _, _ -> true }
        assertTrue(selection.isEmpty())
    }
}
