package org.samvid.orchit.engine

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LocalProjectStorageTest {
    @get:Rule val temp = TemporaryFolder()
    private fun storage() = LocalProjectStorage(temp.root)

    @Test fun createListLoadSaveRenameDelete() {
        val store = storage()
        val created = store.create("Draft")
        assertEquals("Draft", store.load(created.id)?.name)
        assertEquals(listOf(created.id), store.list().map { it.id })
        val edited = created.copy(tracks = listOf(Track("v1", "video", emptyList())))
        store.save(edited)
        assertEquals(edited, store.load(created.id))
        assertEquals("Renamed", store.rename(created.id, "Renamed").name)
        assertEquals("Renamed", store.list().single().name)
        assertTrue(store.delete(created.id))
        assertNull(store.load(created.id))
        assertTrue(store.list().isEmpty())
    }

    @Test fun corruptFilesAreIgnoredByListAndRejectedByLoad() {
        val store = storage()
        val p = store.create("Good")
        java.io.File(temp.root, "${p.id.value}.json").writeText("{broken")
        assertTrue(store.list().isEmpty())
        assertThrows(RuntimeException::class.java) { store.load(p.id) }
    }
}
