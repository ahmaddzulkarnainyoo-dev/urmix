package com.winatra.urmix.remoteconfig

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * P0 §1.2: remote-config payload parsing.
 *
 * Supabase/PostgREST answers table queries with a JSON *array*, while
 * single-row responses and hand-written fixtures are plain objects, so both
 * envelopes must parse into the same config. Malformed payloads (captive
 * portals, HTML error pages, truncated bodies) must fail soft so the app keeps
 * serving its cached config.
 */
class RemoteConfigPayloadTest {

    @Test
    fun `parses postgrest array envelope and maps every field`() {
        val config = RemoteConfigRepository.parseConfig(POSTGREST_PAYLOAD)
        assertNotNull(config)
        requireNotNull(config)

        assertEquals(1, config.configSchemaVersion)
        assertEquals("1.0.0", config.appVersion)
        assertEquals("0.29.1", config.minExtractorVersion)
        assertFalse(config.forceUpdate)
        assertEquals(RELEASE_URL, config.updateUrl)

        assertTrue(config.announcement?.active == true)
        assertEquals("Halo URMIX", config.announcement?.title)
        assertEquals("Pesan pengumuman", config.announcement?.message)

        assertEquals("Dukung URMIX", config.donation?.title)
        assertEquals("https://saweria.co/winatra", config.donation?.saweriaUrl)
        assertNull(config.donation?.qrisUrl)
        assertEquals("Terima kasih", config.donation?.dailyMessage)

        assertEquals(listOf("https://www.youtube.com/@TED"), config.podcastChannels)
        assertEquals(listOf(PLAYLIST_URL), config.podcastPlaylists)
        // channels first, then playlists (blueprint v2 §3.3)
        assertEquals(listOf("https://www.youtube.com/@TED", PLAYLIST_URL), config.podcastSources)
    }

    @Test
    fun `parses plain object envelope`() {
        val config = RemoteConfigRepository.parseConfig(
            """{ "config_schema_version": 2, "app_version": "1.1.0", "force_update": true }"""
        )
        assertNotNull(config)
        requireNotNull(config)

        assertEquals(2, config.configSchemaVersion)
        assertEquals("1.1.0", config.appVersion)
        assertTrue(config.forceUpdate)
    }

    @Test
    fun `empty payloads yield no config`() {
        // An empty array is what PostgREST returns when RLS blocks the row.
        assertNull(RemoteConfigRepository.parseConfig("[]"))
        assertNull(RemoteConfigRepository.parseConfig("[ ]"))
        assertNull(RemoteConfigRepository.parseConfig("   "))
        assertNull(RemoteConfigRepository.parseConfig(""))
    }

    @Test
    fun `malformed payloads fail soft`() {
        assertNull(RemoteConfigRepository.parseConfig("{ not json"))
        assertNull(RemoteConfigRepository.parseConfig("[{\"a\":]"))
        assertNull(RemoteConfigRepository.parseConfig("<!DOCTYPE html><html></html>"))
        assertNull(RemoteConfigRepository.parseConfig("null"))
    }

    @Test
    fun `missing sections stay empty`() {
        val config = RemoteConfigRepository.parseConfig("""[{"config_schema_version": 1}]""")
        assertNotNull(config)
        requireNotNull(config)

        assertEquals(1, config.configSchemaVersion)
        assertNull(config.appVersion)
        assertNull(config.updateUrl)
        assertNull(config.announcement)
        assertNull(config.donation)
        assertTrue(config.podcastSources.isEmpty())
    }

    @Test
    fun `non http update url is dropped`() {
        val config = RemoteConfigRepository.parseConfig(
            """[{ "config_schema_version": 1, "update_url": "javascript:alert(1)" }]"""
        )
        assertNotNull(config)
        requireNotNull(config)

        assertNull(config.updateUrl)
    }

    @Test
    fun `invalid podcast entries are filtered out`() {
        val config = RemoteConfigRepository.parseConfig(
            """[{
                 "config_schema_version": 1,
                 "podcast_channels": ["https://www.youtube.com/@TED", "not-a-url"],
                 "podcast_playlists": ["ftp://example.com/list"]
               }]"""
        )
        assertNotNull(config)
        requireNotNull(config)

        assertEquals(listOf("https://www.youtube.com/@TED"), config.podcastChannels)
        assertTrue(config.podcastPlaylists.isEmpty())
        assertEquals(listOf("https://www.youtube.com/@TED"), config.podcastSources)
    }

    private companion object {
        const val RELEASE_URL =
            "https://github.com/ahmaddzulkarnainyoo-dev/urmix/releases/latest"
        const val PLAYLIST_URL = "https://www.youtube.com/playlist?list=PL1234567890"

        /** Exactly the shape Supabase returns for `?select=*&limit=1`. */
        val POSTGREST_PAYLOAD = """
            [
              {
                "config_schema_version": 1,
                "app_version": "1.0.0",
                "min_extractor_version": "0.29.1",
                "force_update": false,
                "update_url": "$RELEASE_URL",
                "announcement": {
                  "active": true,
                  "title": "Halo URMIX",
                  "message": "Pesan pengumuman"
                },
                "donation": {
                  "title": "Dukung URMIX",
                  "saweria_url": "https://saweria.co/winatra",
                  "qris_url": null,
                  "daily_message": "Terima kasih"
                },
                "podcast_channels": ["https://www.youtube.com/@TED"],
                "podcast_playlists": ["$PLAYLIST_URL"]
              }
            ]
        """.trimIndent()
    }
}
