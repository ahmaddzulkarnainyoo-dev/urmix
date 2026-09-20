package com.winatra.urmix.local.playlist

import com.winatra.urmix.database.AppDatabase
import com.winatra.urmix.database.stream.model.StreamEntity
import com.winatra.urmix.testUtil.TestDatabase
import com.winatra.urmix.testUtil.TrampolineSchedulerRule
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.schabi.newpipe.extractor.stream.StreamType

class LocalPlaylistManagerTest {

    private lateinit var manager: LocalPlaylistManager
    private lateinit var database: AppDatabase

    @get:Rule
    val trampolineScheduler = TrampolineSchedulerRule()

    @Before
    fun setup() {
        database = TestDatabase.createReplacingNewPipeDatabase()
        manager = LocalPlaylistManager(database)
    }

    @After
    fun cleanUp() {
        database.close()
    }

    @Test
    fun createPlaylist() {
        val NEWPIPE_URL = "https://example.com/"
        val stream = StreamEntity(
            serviceId = 1,
            url = NEWPIPE_URL,
            title = "title",
            streamType = StreamType.VIDEO_STREAM,
            duration = 1,
            uploader = "uploader",
            uploaderUrl = NEWPIPE_URL
        )

        val result = manager.createPlaylist("name", listOf(stream))

        // This should not behave like this.
        // Currently list of all stream ids is returned instead of playlist id
        result.test().await().assertValue(listOf(1L))
    }

    @Test
    fun createPlaylist_emptyPlaylistMustReturnEmpty() {
        val result = manager.createPlaylist("name", emptyList())

        // This should not behave like this.
        // It should throw an error because currently the result is null
        result.test().await().assertComplete()
        manager.playlists.test().awaitCount(1).assertValue(emptyList())
    }

    @Test()
    fun createPlaylist_nonExistentStreamsAreUpserted() {
        val stream = StreamEntity(
            serviceId = 1,
            url = "https://example.com/",
            title = "title",
            streamType = StreamType.VIDEO_STREAM,
            duration = 1,
            uploader = "uploader",
            uploaderUrl = "https://example.com/"
        )
        database.streamDAO().insert(stream)
        val upserted = StreamEntity(
            serviceId = 1,
            url = "https://example.com/2",
            title = "title2",
            streamType = StreamType.VIDEO_STREAM,
            duration = 1,
            uploader = "uploader",
            uploaderUrl = "https://example.com/"
        )

        val result = manager.createPlaylist("name", listOf(stream, upserted))

        result.test().await().assertComplete()
        database.streamDAO().getAll().test().awaitCount(1).assertValue(listOf(stream, upserted))
    }
}
