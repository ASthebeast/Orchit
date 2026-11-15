package io.github.asthebeast.orchit.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimelineMediaMappingTest {
    @Test fun mapsVideoClipsInTimelineOrderWithTrimmedSourceRanges() {
        val project = Project(
            ProjectId("p"), "Test", listOf(
                Track("audio", "audio", listOf(clip("a", "audio://a", 0, 0, 900))),
                Track("video", "video", listOf(
                    clip("late", "content://late", 2_000, 500, 1_500),
                    clip("early", "content://early", 0, 1_000, 2_000),
                )),
            ),
        )
        assertEquals(
            listOf(
                TimelineMediaSegment("early", "content://early", 1_000, 3_000, 2_000),
                TimelineMediaSegment("late", "content://late", 500, 2_000, 1_500),
            ),
            TimelineMediaMapping.map(project),
        )
    }

    @Test fun positionLogicMapsPlaylistPositionDurationAndSeek() {
        val logic = TimelinePositionLogic(listOf(
            TimelineMediaSegment("a", "a", 100, 1_100, 1_000),
            TimelineMediaSegment("b", "b", 0, 2_000, 2_000),
        ))
        assertEquals(3_000L, logic.durationUs)
        assertEquals(1_400L, logic.timelinePosition(1, 400))
        assertEquals(TimelineSeekLocation(0, 0), logic.locate(-20))
        assertEquals(TimelineSeekLocation(1, 500), logic.locate(1_500))
        assertEquals(TimelineSeekLocation(1, 2_000), logic.locate(4_000))
        assertNull(TimelinePositionLogic(emptyList()).locate(0))
    }

    private fun clip(id: String, uri: String, start: Long, sourceIn: Long, duration: Long) = Clip(
        id, MediaReference(uri, "video/mp4"), TimelineTimeUs(start), TimelineTimeUs(sourceIn), TimelineTimeUs(duration),
    )
}
