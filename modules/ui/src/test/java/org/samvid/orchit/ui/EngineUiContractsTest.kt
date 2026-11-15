package org.samvid.orchit.ui

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.samvid.orchit.engine.*

class EngineUiContractsTest {
    @get:Rule val temp = TemporaryFolder()

    @Test fun projectListStateTracksStorageChanges() {
        val contract = StoredProjectListContract(LocalProjectStorage(temp.root))
        val project = contract.createProject("One")
        assertEquals(listOf("One"), contract.state.value.projects.map { it.name })
        assertTrue(contract.openProject(project.id))
        assertEquals(project.id, contract.state.value.openedProjectId)
        contract.renameProject(project.id, "Two")
        assertEquals("Two", contract.state.value.projects.single().name)
        contract.deleteProject(project.id)
        assertTrue(contract.state.value.projects.isEmpty())
        assertNull(contract.state.value.openedProjectId)
    }

    @Test fun editorActionsUpdateObservableState() {
        val editor = EngineEditorUiContract(TimelineEditor(Project(ProjectId("p"), "Cut", emptyList())))
        editor.dispatch(EditorAction.ScrubStarted(45))
        assertEquals(45L, editor.state.value.playheadUs)
        assertTrue(editor.state.value.isScrubbing)
        editor.dispatch(EditorAction.SelectTrack("v1"))
        assertEquals("v1", editor.state.value.selectedTrackId)
        editor.dispatch(EditorAction.Pause)
        assertFalse(editor.state.value.isPlaying)
    }
}
