package com.winatra.urmix.settings.export
// URMIX library backup: export side (blueprint v2 §5.2). Part 1:
// context readers + track/entity converters.
import android.content.Context
import com.winatra.urmix.R
import com.winatra.urmix.database.AppDatabase
import com.winatra.urmix.database.history.model.StreamHistoryEntry
import com.winatra.urmix.database.playlist.PlaylistMetadataEntry
import com.winatra.urmix.database.playlist.PlaylistStreamEntry
import com.winatra.urmix.database.playlist.model.PlaylistEntity
import com.winatra.urmix.database.playlist.model.PlaylistRemoteEntity
import com.winatra.urmix.database.stream.model.StreamEntity
import java.time.OffsetDateTime

internal fun trackFromStreamEntity(stream: StreamEntity): BackupTrack {
    return BackupTrack(
        serviceId = stream.serviceId,
        url = stream.url,
        title = stream.title,
        uploader = stream.uploader.takeIf { it.isNotEmpty() },
        uploaderUrl = stream.uploaderUrl,
        thumbnailUrl = stream.thumbnailUrl,
        duration = stream.duration,
        streamType = stream.streamType,
        textualUploadDate = stream.textualUploadDate
    )
}

internal fun streamEntityFromTrack(track: BackupTrack): StreamEntity? {
    if (!track.hasIdentity()) {
        return null
    }
    return StreamEntity(
        serviceId = track.serviceId,
        url = track.url,
        title = track.title,
        streamType = track.streamType,
        duration = track.duration,
        uploader = track.uploader ?: "",
        uploaderUrl = track.uploaderUrl,
        thumbnailUrl = track.thumbnailUrl,
        textualUploadDate = track.textualUploadDate
    )
}

internal fun playlistEntityFromBackup(
    index: Long,
    name: String,
    streams: List<Long>
): PlaylistEntity {
    return PlaylistEntity(
        name = name,
        isThumbnailPermanent = false,
        thumbnailStreamId = streams.firstOrNull()
            ?: PlaylistEntity.DEFAULT_THUMBNAIL_ID,
        displayIndex = index
    )
}

fun defaultPlaylistsOf(
    database: AppDatabase
): List<PlaylistMetadataEntry> {
    return database.playlistStreamDAO().getPlaylistMetadata()
        .blockingFirst(mutableListOf<PlaylistMetadataEntry>())
}

internal fun orderedStreamsOf(
    database: AppDatabase,
    playlistId: Long
): List<PlaylistStreamEntry> {
    return database.playlistStreamDAO()
        .getOrderedStreamsOf(playlistId).blockingFirst(mutableListOf<PlaylistStreamEntry>())
}

internal fun nowIso(): String = OffsetDateTime.now().toString()
internal fun exportHistory(
    database: AppDatabase
): List<BackupHistoryEntry> {
    val entries = database.streamHistoryDAO().history
        .blockingFirst(mutableListOf<StreamHistoryEntry>())
    return entries.map { entry ->
        BackupHistoryEntry(
            track = entry.toStreamInfoItem().toBackupTrack(),
            accessDate = entry.accessDate,
            repeatCount = entry.repeatCount.coerceAtLeast(1L)
        )
    }
}

internal fun org.schabi.newpipe.extractor.stream.StreamInfoItem.toBackupTrack(): BackupTrack {
    return BackupTrack(
        serviceId = serviceId,
        url = url,
        title = name,
        uploader = uploaderName?.takeIf { it.isNotEmpty() },
        uploaderUrl = uploaderUrl?.takeIf { it.isNotEmpty() },
        thumbnailUrl = com.winatra.urmix.util.image.ImageStrategy.imageListToDbUrl(thumbnails),
        duration = duration,
        streamType = streamType
    )
}

internal fun exportLocalPlaylists(
    context: Context,
    database: AppDatabase,
    excludeLikedUid: Long
): List<BackupPlaylist> {
    val result = ArrayList<BackupPlaylist>()
    defaultPlaylistsOf(database).forEachIndexed { index, meta ->
        if (meta.uid == excludeLikedUid) {
            return@forEachIndexed
        }
        val tracks = orderedStreamsOf(database, meta.uid).map {
            trackFromStreamEntity(it.streamEntity)
        }
        result.add(
            BackupPlaylist(
                name = meta.orderingName ?: "",
                playlistType = "LOCAL",
                thumbnailUrl = meta.thumbnailUrl,
                displayIndex = index.toLong(),
                tracks = tracks
            )
        )
    }
    return result
}

internal fun exportRemoteBookmarks(
    database: AppDatabase
): List<BackupPlaylist> {
    return database.playlistRemoteDAO().playlists
        .blockingFirst(mutableListOf<PlaylistRemoteEntity>()).mapIndexed { index, remote ->
            BackupPlaylist(
                name = remote.orderingName ?: "",
                playlistType = "REMOTE",
                serviceId = remote.serviceId,
                url = remote.url,
                uploader = remote.uploader,
                thumbnailUrl = remote.thumbnailUrl,
                displayIndex = index.toLong(),
                tracks = emptyList()
            )
        }
}

internal fun likedPlaylistName(context: Context): String {
    return context.getString(R.string.liked_songs_playlist_name)
}

internal fun exportSearchHistory(
    database: AppDatabase
): List<String> {
    return database.searchHistoryDAO().getAll()
        .blockingFirst(emptyList())
        .mapNotNull { it.search?.takeIf { q -> q.isNotEmpty() } }
}

internal fun readLikedUid(context: Context): Long {
    return context.getSharedPreferences(BackupSchema.PREFS_FILE_NAME, Context.MODE_PRIVATE)
        .getLong(BackupSchema.LIKED_PLAYLIST_UID_PREF, -1L)
}

fun saveLikedUid(context: Context, uid: Long) {
    context.getSharedPreferences(BackupSchema.PREFS_FILE_NAME, Context.MODE_PRIVATE)
        .edit().putLong(BackupSchema.LIKED_PLAYLIST_UID_PREF, uid).apply()
}

/**
 * Pure helper for [resolveLikedUid]: decide which UID should be stored.
 * prefsUid = value from SharedPreferences, metas = current playlists metadata.
 */
internal fun pickLikedUid(
    prefsUid: Long,
    metas: List<PlaylistMetadataEntry>,
    likedName: String
): Long {
    if (prefsUid >= 0 && metas.any { it.uid == prefsUid }) {
        return prefsUid
    }
    return metas.firstOrNull { it.orderingName == likedName }?.uid ?: -1L
}

/**
 * Self-heal the stored Liked Songs UID (Option A + D).
 * If the stored UID no longer exists (e.g. after a legacy ZIP import that
 * replaced newpipe.db), fall back to finding the playlist by name.
 */
fun resolveLikedUid(context: Context, database: AppDatabase): Long {
    val appContext = context.applicationContext
    val prefsUid = readLikedUid(appContext)
    val metas = try {
        defaultPlaylistsOf(database)
    } catch (e: Exception) {
        emptyList()
    }
    val resolved = pickLikedUid(prefsUid, metas, likedPlaylistName(appContext))
    if (resolved != prefsUid) {
        saveLikedUid(appContext, resolved)
    }
    return resolved
}

internal fun exportLikedTracks(
    context: Context,
    database: AppDatabase,
    likedUid: Long
): List<BackupTrack> {
    if (likedUid < 0) {
        return emptyList()
    }
    return orderedStreamsOf(database, likedUid).map {
        trackFromStreamEntity(it.streamEntity)
    }.take(BackupSchema.MAX_TRACKS_PER_PLAYLIST)
}

object LibraryBackupExporter {

    @androidx.annotation.WorkerThread
    fun build(context: Context, database: AppDatabase): UrmixBackup {
        val appContext = context.applicationContext
        val likedUid = resolveLikedUid(appContext, database)
        return UrmixBackup(
            schemaVersion = BackupSchema.VERSION,
            exportedAt = nowIso(),
            appName = "URMIX",
            appVersion = com.winatra.urmix.BuildConfig.VERSION_NAME,
            likedTracks = exportLikedTracks(appContext, database, likedUid),
            playlists = exportLocalPlaylists(appContext, database, likedUid) +
                exportRemoteBookmarks(database),
            history = exportHistory(database).take(BackupSchema.MAX_HISTORY_ENTRIES),
            searchHistory = exportSearchHistory(database).take(BackupSchema.MAX_SEARCH_ENTRIES)
        )
    }
}
