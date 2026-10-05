package com.don.homefitness.data.catalog

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaLifecycleTest {
    @Test
    fun leavingDetailStopsAndReleasesCurrentGif() {
        val player = RecordingGifPlayer()
        val controller = MediaPlaybackController(player)

        controller.onDetailVisible("0001")
        controller.onDetailHidden()

        assertEquals(listOf("play:0001", "stop", "release"), player.events)
    }

    @Test
    fun backgroundStopsAndReleasesWithoutListAutoplay() {
        val player = RecordingGifPlayer()
        val controller = MediaPlaybackController(player)

        controller.onListVisible()
        controller.onBackground()

        assertEquals(emptyList<String>(), player.events)
    }

    private class RecordingGifPlayer : GifPlayer {
        val events = mutableListOf<String>()

        override fun play(gifPath: String) { events += "play:$gifPath" }
        override fun stop() { events += "stop" }
        override fun release() { events += "release" }
    }
}
