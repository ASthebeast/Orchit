package org.samvid.orchit.engine

/** Platform independent source segment produced from a timeline video clip. */
data class TimelineMediaSegment(
    val clipId: String,
    val uri: String,
    val sourceStartUs: Long,
    val sourceEndUs: Long,
    val durationUs: Long,
)

data class TimelineSeekLocation(val itemIndex: Int, val positionInItemUs: Long)

/** Maps the first video track to a continuous Media3 playlist, preserving timeline clip order and trims. */
object TimelineMediaMapping {
    fun map(project: Project): List<TimelineMediaSegment> = project.tracks.firstOrNull { it.kind == "video" }
        ?.clips.orEmpty().sortedBy { it.start.value }.map { clip ->
            require(clip.sourceIn.value >= 0 && clip.duration.value > 0) { "Video clip trim must be valid" }
            TimelineMediaSegment(clip.id, clip.media.uri, clip.sourceIn.value, clip.sourceIn.value + clip.duration.value, clip.duration.value)
        }
}

/** Pure duration, position, and seek calculations for a flattened video playlist. */
class TimelinePositionLogic(private val segments: List<TimelineMediaSegment>) {
    val durationUs: Long = segments.sumOf { it.durationUs }

    fun timelinePosition(itemIndex: Int, positionInItemUs: Long): Long {
        if (segments.isEmpty()) return 0L
        val index = itemIndex.coerceIn(0, segments.lastIndex)
        return segments.take(index).sumOf { it.durationUs } + positionInItemUs.coerceIn(0L, segments[index].durationUs)
    }

    fun locate(positionUs: Long): TimelineSeekLocation? {
        if (segments.isEmpty()) return null
        var remaining = positionUs.coerceIn(0L, durationUs)
        segments.forEachIndexed { index, segment ->
            if (remaining < segment.durationUs || index == segments.lastIndex) {
                return TimelineSeekLocation(index, remaining.coerceAtMost(segment.durationUs))
            }
            remaining -= segment.durationUs
        }
        return null
    }
}
