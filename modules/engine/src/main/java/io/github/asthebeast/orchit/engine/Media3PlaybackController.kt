package io.github.asthebeast.orchit.engine

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** ExoPlayer-backed playback for a project's flattened video playlist. */
class Media3PlaybackController(
    context: Context,
    project: Project,
    val player: ExoPlayer = ExoPlayer.Builder(context).build(),
) : PlaybackController {
    private var segments = TimelineMediaMapping.map(project)
    private var logic = TimelinePositionLogic(segments)
    private val mutableState = MutableStateFlow(PlaybackState(0L, logic.durationUs, false))
    override val state: StateFlow<PlaybackState> = mutableState
    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            updateState()
            if (player.isPlaying) handler.postDelayed(this, 33L)
        }
    }
    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updateState()
            handler.removeCallbacks(ticker)
            if (isPlaying) handler.post(ticker)
        }
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = updateState()
        override fun onPlaybackStateChanged(playbackState: Int) = updateState()
    }

    init {
        player.addListener(listener)
        player.setMediaItems(segments.map(::mediaItem))
        player.prepare()
        updateState()
    }

    override fun play() { if (segments.isNotEmpty()) player.play() }
    override fun pause() = player.pause()
    override fun position() = TimelineTimeUs(mutableState.value.positionUs)

    override fun seek(position: TimelineTimeUs) {
        val target = logic.locate(position.value) ?: return
        player.seekTo(target.itemIndex, target.positionInItemUs / 1_000L)
        updateState()
    }

    /** Rebuilds the playlist after the editor changes project clips. */
    override fun replaceProject(project: Project) {
        segments = TimelineMediaMapping.map(project)
        logic = TimelinePositionLogic(segments)
        player.setMediaItems(segments.map(::mediaItem))
        player.prepare()
        updateState()
    }

    private fun mediaItem(segment: TimelineMediaSegment): MediaItem = MediaItem.Builder()
        .setMediaId(segment.clipId)
        .setUri(Uri.parse(segment.uri))
        .setClippingConfiguration(
            MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(segment.sourceStartUs / 1_000L)
                .setEndPositionMs(segment.sourceEndUs / 1_000L)
                .build(),
        )
        .build()

    private fun updateState() {
        val index = player.currentMediaItemIndex
        val positionUs = if (index >= 0 && index < segments.size) {
            logic.timelinePosition(index, player.currentPosition.coerceAtLeast(0L) * 1_000L)
        } else if (player.playbackState == Player.STATE_ENDED) logic.durationUs else 0L
        mutableState.value = PlaybackState(positionUs.coerceIn(0L, logic.durationUs), logic.durationUs, player.isPlaying)
    }

    override fun close() {
        handler.removeCallbacks(ticker)
        player.removeListener(listener)
        player.release()
    }
}
