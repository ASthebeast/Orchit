package org.samvid.orchit.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.samvid.orchit.engine.LocalProjectStorage
import org.samvid.orchit.engine.ProjectId
import org.samvid.orchit.engine.TimelineEditor
import org.samvid.orchit.engine.Media3PlaybackController
import org.samvid.orchit.engine.PlaybackController
import org.samvid.orchit.engine.PlaybackState
import org.samvid.orchit.engine.Clip
import org.samvid.orchit.engine.MediaReference
import org.samvid.orchit.engine.TimelineTimeUs
import org.samvid.orchit.engine.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import androidx.media3.common.Player
import java.util.UUID

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
class EngineEditorUiContract(
    private val editor: TimelineEditor,
    private val storage: LocalProjectStorage,
    private val playback: PlaybackController,
    dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) : EditorUiContract, AutoCloseable {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val mutableState = MutableStateFlow(editorState(playback.state.value))
    override val state: StateFlow<EditorUiState> = mutableState
    override val previewPlayer: Player? get() = (playback as? Media3PlaybackController)?.player

    init {
        scope.launch { playback.state.collect { value -> mutableState.value = editorState(value, mutableState.value) } }
    }

    override fun dispatch(action: EditorAction) {
        val old = mutableState.value
        when (action) {
            EditorAction.Play -> { playback.play(); mutableState.value = old.copy(isPlaying = true) }
            EditorAction.Pause -> { playback.pause(); mutableState.value = old.copy(isPlaying = false) }
            is EditorAction.Seek -> seekTo(action.targetPositionUs)
            is EditorAction.ScrubStarted -> { playback.pause(); seekTo(action.targetPositionUs); mutableState.value = mutableState.value.copy(isScrubbing = true, isPlaying = false) }
            is EditorAction.ScrubTo -> seekTo(action.targetPositionUs)
            is EditorAction.ScrubEnded -> { seekTo(action.targetPositionUs); mutableState.value = mutableState.value.copy(isScrubbing = false) }
            is EditorAction.SelectClip -> mutableState.value = old.copy(selectedClipId = action.clipId, selectedTrackId = null)
            EditorAction.DeselectClip -> mutableState.value = old.copy(selectedClipId = null)
            is EditorAction.SelectTrack -> mutableState.value = old.copy(selectedTrackId = action.trackId, selectedClipId = null)
            EditorAction.DeselectTrack -> mutableState.value = old.copy(selectedTrackId = null)
            is EditorAction.ImportVideo -> importVideo(action)
        }
    }

    private fun seekTo(targetPositionUs: Long) {
        playback.seek(TimelineTimeUs(targetPositionUs))
        mutableState.value = mutableState.value.copy(playheadUs = targetPositionUs.coerceIn(0L, playback.state.value.durationUs))
    }

    private fun importVideo(action: EditorAction.ImportVideo) {
        require(action.durationUs > 0L) { "Video duration must be positive" }
        var videoTrack = editor.project.tracks.firstOrNull { it.kind == "video" }
        if (videoTrack == null) {
            val track = Track("video-${UUID.randomUUID()}", "video", emptyList())
            editor.addTrack(track)
            videoTrack = track
        }
        val start = editor.project.duration.value
        val clip = Clip(
            id = UUID.randomUUID().toString(),
            media = MediaReference(action.uri, action.mimeType, "NOASSERTION"),
            start = TimelineTimeUs(start),
            sourceIn = TimelineTimeUs(0),
            duration = TimelineTimeUs(action.durationUs),
        )
        editor.addClip(videoTrack.id, clip)
        storage.save(editor.project)
        playback.replaceProject(editor.project)
        mutableState.value = mutableState.value.copy(projectName = editor.project.name, durationUs = playback.state.value.durationUs)
    }

    private fun editorState(value: PlaybackState, previous: EditorUiState? = null) = EditorUiState(
        projectName = editor.project.name,
        isPlaying = value.isPlaying,
        playheadUs = value.positionUs,
        durationUs = value.durationUs,
        selectedClipId = previous?.selectedClipId,
        selectedTrackId = previous?.selectedTrackId,
        isScrubbing = previous?.isScrubbing ?: false,
    )

    override fun close() { scope.cancel(); playback.close() }
}
