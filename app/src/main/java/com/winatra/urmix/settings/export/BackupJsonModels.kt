package com.winatra.urmix.settings.export
// URMIX manual library backup — JSON schema v1 (blueprint v2 §5.2). Part 1:
// schema constants, BackupTrack model, safe nanojson readers.
import com.grack.nanojson.JsonObject
import com.grack.nanojson.JsonStringWriter
import org.schabi.newpipe.extractor.stream.StreamType

object BackupSchema {
    const val VERSION = 1
    const val KEY_SCHEMA_VERSION = "backup_schema_version"
    const val KEY_EXPORTED_AT = "exported_at"
    const val KEY_APP_NAME = "app_name"
    const val KEY_APP_VERSION = "app_version"
    const val KEY_LIKED_TRACKS = "liked_tracks"
    const val KEY_PLAYLISTS = "playlists"
    const val KEY_HISTORY = "history"
    const val KEY_SEARCH_HISTORY = "search_history"
    const val FILE_NAME_PREFIX = "urmix_backup_"
    const val LIKED_PLAYLIST_UID_PREF = "urmix_liked_songs_playlist_uid"
    const val PREFS_FILE_NAME = "urmix_backup_prefs"
    const val JSON_MIME_TYPE = "application/json"
    const val MAX_HISTORY_ENTRIES = 5000
    const val MAX_TRACKS_PER_PLAYLIST = 5000
    const val MAX_SEARCH_ENTRIES = 200
}

data class BackupTrack(
    val serviceId: Int = -1,
    val url: String = "",
    val title: String = "",
    val uploader: String? = null,
    val uploaderUrl: String? = null,
    val thumbnailUrl: String? = null,
    val duration: Long = -1L,
    val streamType: StreamType = StreamType.VIDEO_STREAM,
    val textualUploadDate: String? = null
) {
    fun hasIdentity(): Boolean = serviceId >= 0 && url.isNotEmpty()
    fun writeFieldsTo(writer: JsonStringWriter) {
        writer
            .value("service_id", serviceId)
            .value("url", url)
            .value("title", title)
            .value("uploader", uploader)
            .value("uploader_url", uploaderUrl)
            .value("thumbnail_url", thumbnailUrl)
            .value("duration", duration)
            .value("stream_type", streamType.name)
            .value("textual_upload_date", textualUploadDate)
    }
    fun writeTo(writer: JsonStringWriter) {
        writer.`object`()
        writeFieldsTo(writer)
        writer.end()
    }
    companion object {
        fun from(json: JsonObject?): BackupTrack? {
            if (json == null) {
                return null
            }
            return BackupTrack(
                serviceId = safeInt(json, "service_id", -1),
                url = safeString(json, "url") ?: return null,
                title = safeString(json, "title") ?: "",
                uploader = safeString(json, "uploader"),
                uploaderUrl = safeString(json, "uploader_url"),
                thumbnailUrl = safeString(json, "thumbnail_url"),
                duration = safeLong(json, "duration", -1L),
                streamType = parseStreamType(safeString(json, "stream_type")),
                textualUploadDate = safeString(json, "textual_upload_date")
            )
        }
    }
}

internal fun safeString(json: JsonObject, key: String): String? {
    return try {
        json.getString(key, null)?.takeIf { it.isNotEmpty() }
    } catch (e: Exception) {
        null
    }
}

internal fun safeInt(json: JsonObject, key: String, fallback: Int): Int {
    return try {
        json.getInt(key, fallback)
    } catch (e: Exception) {
        fallback
    }
}

internal fun safeLong(json: JsonObject, key: String, fallback: Long): Long {
    return try {
        json.getLong(key, fallback)
    } catch (e: Exception) {
        fallback
    }
}

internal fun parseStreamType(raw: String?): StreamType {
    if (raw.isNullOrEmpty()) {
        return StreamType.VIDEO_STREAM
    }
    return try {
        StreamType.valueOf(raw)
    } catch (e: Exception) {
        StreamType.VIDEO_STREAM
    }
}

internal fun parseDate(raw: String?): java.time.OffsetDateTime? {
    if (raw.isNullOrEmpty()) {
        return null
    }
    return try {
        java.time.OffsetDateTime.parse(raw)
    } catch (e: Exception) {
        null
    }
}

data class BackupPlaylist(
    val name: String = "",
    val playlistType: String = "LOCAL",
    val serviceId: Int = -1,
    val url: String? = null,
    val uploader: String? = null,
    val thumbnailUrl: String? = null,
    val displayIndex: Long = 0L,
    val tracks: List<BackupTrack> = emptyList()
) {
    fun isRemote(): Boolean = playlistType == "REMOTE"
    fun writeFieldsTo(writer: JsonStringWriter) {
        writer
            .value("name", name)
            .value("playlist_type", playlistType)
            .value("service_id", serviceId)
            .value("url", url)
            .value("uploader", uploader)
            .value("thumbnail_url", thumbnailUrl)
            .value("display_index", displayIndex)
        writer.array("tracks")
        tracks.forEach { it.writeTo(writer) }
        writer.end()
    }

    fun writeTo(writer: JsonStringWriter) {
        writer.`object`()
        writeFieldsTo(writer)
        writer.end()
    }
    companion object {
        fun from(json: JsonObject?): BackupPlaylist? {
            if (json == null) {
                return null
            }
            val name = safeString(json, "name") ?: return null
            val tracks = ArrayList<BackupTrack>()
            json.getArray("tracks")?.forEach { element ->
                (element as? JsonObject)?.let { BackupTrack.from(it)?.let(tracks::add) }
            }
            return BackupPlaylist(
                name = name,
                playlistType = safeString(json, "playlist_type") ?: "LOCAL",
                serviceId = safeInt(json, "service_id", -1),
                url = safeString(json, "url"),
                uploader = safeString(json, "uploader"),
                thumbnailUrl = safeString(json, "thumbnail_url"),
                displayIndex = safeLong(json, "display_index", 0L),
                tracks = tracks
            )
        }
    }
}

data class BackupHistoryEntry(
    val track: BackupTrack,
    val accessDate: java.time.OffsetDateTime? = null,
    val repeatCount: Long = 1L
) {
    fun writeTo(writer: JsonStringWriter) {
        writer.`object`()
        writer.value("access_date", accessDate?.toString())
        writer.value("repeat_count", repeatCount)
        writer.`object`("track")
        track.writeFieldsTo(writer)
        writer.end()
        writer.end()
    }

    companion object {
        fun from(json: JsonObject?): BackupHistoryEntry? {
            if (json == null) {
                return null
            }
            val track = BackupTrack.from(json.getObject("track")) ?: return null
            return BackupHistoryEntry(
                track = track,
                accessDate = parseDate(safeString(json, "access_date")),
                repeatCount = safeLong(json, "repeat_count", 1L).coerceAtLeast(1L)
            )
        }
    }
}

data class UrmixBackup(
    val schemaVersion: Int = BackupSchema.VERSION,
    val exportedAt: String? = null,
    val appName: String? = null,
    val appVersion: String? = null,
    val likedTracks: List<BackupTrack> = emptyList(),
    val playlists: List<BackupPlaylist> = emptyList(),
    val history: List<BackupHistoryEntry> = emptyList(),
    val searchHistory: List<String> = emptyList()
) {
    companion object {
        fun from(root: JsonObject?): UrmixBackup {
            if (root == null) {
                throw BackupJsonException("Backup file is empty or invalid JSON")
            }
            val version = safeInt(root, BackupSchema.KEY_SCHEMA_VERSION, -1)
            if (version != BackupSchema.VERSION) {
                throw BackupJsonException(
                    "Unsupported backup_schema_version: $version " +
                        "(expected ${BackupSchema.VERSION})"
                )
            }
            val liked = ArrayList<BackupTrack>()
            root.getArray(BackupSchema.KEY_LIKED_TRACKS)?.forEach { element ->
                (element as? JsonObject)?.let { BackupTrack.from(it)?.let(liked::add) }
            }
            val playlists = ArrayList<BackupPlaylist>()
            root.getArray(BackupSchema.KEY_PLAYLISTS)?.forEach { element ->
                (element as? JsonObject)?.let { BackupPlaylist.from(it)?.let(playlists::add) }
            }
            val history = ArrayList<BackupHistoryEntry>()
            root.getArray(BackupSchema.KEY_HISTORY)?.forEach { element ->
                (element as? JsonObject)?.let { BackupHistoryEntry.from(it)?.let(history::add) }
            }
            val search = ArrayList<String>()
            root.getArray(BackupSchema.KEY_SEARCH_HISTORY)?.forEach { element ->
                (element as? String)?.takeIf { it.isNotEmpty() }?.let(search::add)
            }
            return UrmixBackup(
                schemaVersion = version,
                exportedAt = safeString(root, BackupSchema.KEY_EXPORTED_AT),
                appName = safeString(root, BackupSchema.KEY_APP_NAME),
                appVersion = safeString(root, BackupSchema.KEY_APP_VERSION),
                likedTracks = liked,
                playlists = playlists,
                history = history,
                searchHistory = search
            )
        }
    }
}

data class BackupPreview(
    val likedCount: Int,
    val playlistCount: Int,
    val historyCount: Int
) {
    fun isEmpty(): Boolean = likedCount <= 0 && playlistCount <= 0 && historyCount <= 0
}

fun writeBackupJson(backup: UrmixBackup): String {
    val writer = com.grack.nanojson.JsonWriter.string()
    writer.`object`()
        .value(BackupSchema.KEY_SCHEMA_VERSION, backup.schemaVersion)
        .value(BackupSchema.KEY_EXPORTED_AT, backup.exportedAt)
        .value(BackupSchema.KEY_APP_NAME, backup.appName)
        .value(BackupSchema.KEY_APP_VERSION, backup.appVersion)
    writer.array(BackupSchema.KEY_LIKED_TRACKS)
    backup.likedTracks.forEach { it.writeTo(writer) }
    writer.end()
    writer.array(BackupSchema.KEY_PLAYLISTS)
    backup.playlists.forEach { it.writeTo(writer) }
    writer.end()
    writer.array(BackupSchema.KEY_HISTORY)
    backup.history.forEach { it.writeTo(writer) }
    writer.end()
    writer.array(BackupSchema.KEY_SEARCH_HISTORY)
    backup.searchHistory.forEach { query -> writer.value(query) }
    writer.end()
    writer.end()
    return writer.done()
}
