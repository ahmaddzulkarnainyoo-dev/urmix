package com.winatra.urmix.settings.export;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.grack.nanojson.JsonParser;
import org.junit.Test;
import org.schabi.newpipe.extractor.stream.StreamType;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;

public class BackupJsonRoundTripTest {
    private static BackupTrack track(final String url) {
        return new BackupTrack(0, url, "title-" + url, null, null, null, 10L,
                StreamType.VIDEO_STREAM, null);
    }

    @Test
    public void fullBackupRoundTrip() throws Exception {
        final BackupTrack liked = track("https://example.com/liked");
        final BackupTrack playlistTrack = track("https://example.com/p1");
        final BackupPlaylist playlist = new BackupPlaylist("Mix", "LOCAL", 0, null,
                null, null, 0L, Collections.singletonList(playlistTrack));
        final BackupHistoryEntry history = new BackupHistoryEntry(playlistTrack,
                OffsetDateTime.parse("2026-01-02T03:04:05Z"), 2L);
        final UrmixBackup backup = new UrmixBackup(BackupSchema.VERSION, null, "URMIX",
                "test", Collections.singletonList(liked),
                Collections.singletonList(playlist),
                Collections.singletonList(history),
                Collections.singletonList("lofi mix"));

        final UrmixBackup parsed = UrmixBackup.Companion.from(
                JsonParser.object().from(BackupJsonModelsKt.writeBackupJson(backup)));

        assertEquals(1, parsed.getLikedTracks().size());
        assertEquals("https://example.com/liked", parsed.getLikedTracks().get(0).getUrl());
        assertEquals(1, parsed.getPlaylists().size());
        assertEquals("Mix", parsed.getPlaylists().get(0).getName());
        assertEquals(StreamType.VIDEO_STREAM,
                parsed.getPlaylists().get(0).getTracks().get(0).getStreamType());
        assertEquals(1, parsed.getHistory().size());
        assertEquals(2L, parsed.getHistory().get(0).getRepeatCount());
        assertEquals(Arrays.asList("lofi mix"), parsed.getSearchHistory());
    }

    @Test
    public void wrongSchemaVersionIsRejected() throws Exception {
        try {
            UrmixBackup.Companion.from(JsonParser.object().from(
                    "{\"backup_schema_version\":999}"));
            throw new AssertionError("expected BackupJsonException");
        } catch (final BackupJsonException expected) {
        }
    }

    @Test
    public void missingKeysFallBackToDefaults() throws Exception {
        final UrmixBackup parsed = UrmixBackup.Companion.from(JsonParser.object().from(
                "{\"backup_schema_version\":1,"
                        + "\"liked_tracks\":[{\"url\":\"https://example.com/x\"}]}"));
        assertEquals(1, parsed.getLikedTracks().size());
        assertEquals(-1, parsed.getLikedTracks().get(0).getServiceId());
        assertEquals(StreamType.VIDEO_STREAM,
                parsed.getLikedTracks().get(0).getStreamType());
        assertTrue(parsed.getPlaylists().isEmpty());
    }

    @Test
    public void nullRootIsRejected() {
        try {
            UrmixBackup.Companion.from(null);
            throw new AssertionError("expected BackupJsonException");
        } catch (final BackupJsonException expected) {
        }
    }

    @Test
    public void emptyHistoryTrackUrlIsSkipped() throws Exception {
        final UrmixBackup parsed = UrmixBackup.Companion.from(JsonParser.object().from(
                "{\"backup_schema_version\":1,\"history\":[{\"track\":{}}]}"));
        assertTrue(parsed.getHistory().isEmpty());
        assertNull(UrmixBackup.Companion.from(JsonParser.object().from(
                "{\"backup_schema_version\":1}")).getExportedAt());
    }
}
