package org.samvid.orchit.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.samvid.orchit.engine.LocalProjectStorage
import org.samvid.orchit.engine.ProjectId
import org.samvid.orchit.engine.TimelineEditor

/** Storage-backed home contract. */
class StoredProjectListContract(private val storage: LocalProjectStorage) : ProjectListContract {
    private val mutableState = MutableStateFlow(ProjectListState(storage.list().map { ProjectSummary(it.id.value, it.name) }))
    override val state: StateFlow<ProjectListState> = mutableState
    override fun listProjects() = storage.list().map { ProjectSummary(it.id.value, it.name) }.also { mutableState.value = mutableState.value.copy(projects = it) }
    override fun createProject(name: String): ProjectSummary = storage.create(name).let { ProjectSummary(it.id.value, it.name) }.also { mutableState.value = mutableState.value.copy(projects = listProjects()) }
    override fun renameProject(projectId: String, name: String) { storage.rename(ProjectId(projectId), name); listProjects() }
    override fun deleteProject(projectId: String) { storage.delete(ProjectId(projectId)); mutableState.value = mutableState.value.copy(projects = listProjects(), openedProjectId = mutableState.value.openedProjectId.takeUnless { it == projectId }) }
    override fun openProject(projectId: String): Boolean {
        if (storage.load(ProjectId(projectId)) == null) return false
        mutableState.value = mutableState.value.copy(openedProjectId = projectId)
        return true
    }
}

/** State adapter around the engine's immutable timeline editor. */
class EngineEditorUiContract(private val editor: TimelineEditor) : EditorUiContract {
    private val mutableState = MutableStateFlow(EditorUiState(editor.project.name, false, 0L))
    override val state: StateFlow<EditorUiState> = mutableState
    override fun dispatch(action: EditorAction) {
        val old = mutableState.value
        val updated = when (action) {
            EditorAction.Play -> old.copy(isPlaying = true)
            EditorAction.Pause -> old.copy(isPlaying = false)
            is EditorAction.Seek -> old.copy(playheadUs = action.targetPositionUs.coerceIn(0L, editor.project.duration.value))
            is EditorAction.ScrubStarted -> old.copy(playheadUs = action.targetPositionUs.coerceAtLeast(0L), isScrubbing = true)
            is EditorAction.ScrubTo -> old.copy(playheadUs = action.targetPositionUs.coerceAtLeast(0L))
            is EditorAction.ScrubEnded -> old.copy(playheadUs = action.targetPositionUs.coerceAtLeast(0L), isScrubbing = false)
            is EditorAction.SelectClip -> old.copy(selectedClipId = action.clipId, selectedTrackId = null)
            EditorAction.DeselectClip -> old.copy(selectedClipId = null)
            is EditorAction.SelectTrack -> old.copy(selectedTrackId = action.trackId, selectedClipId = null)
            EditorAction.DeselectTrack -> old.copy(selectedTrackId = null)
        }
        mutableState.value = updated
    }
}
