package com.winatra.urmix.settings.export
// URMIX library backup: import side (blueprint v2 §5.2). Part 1:
// parse + preview + history restore helpers.
import android.content.Context
import androidx.annotation.WorkerThread
import com.grack.nanojson.JsonParser
import com.winatra.urmix.NewPipeDatabase
import com.winatra.urmix.database.AppDatabase
import com.winatra.urmix.database.history.model.SearchHistoryEntry
import com.winatra.urmix.database.history.model.StreamHistoryEntity
import com.winatra.urmix.database.playlist.model.PlaylistRemoteEntity
import com.winatra.urmix.database.playlist.model.PlaylistStreamEntity
import com.winatra.urmix.streams.io.SharpInputStream
import com.winatra.urmix.streams.io.StoredFileHelper
import java.time.OffsetDateTime

data class BackupApplyResult(
    val likedCount: Int,
    val playlistCount: Int,
    val historyCount: Int
)

object LibraryBackupImporter {
    fun parse(file: StoredFileHelper): UrmixBackup {
        try {
            val raw = SharpInputStream(file.stream).use { input ->
                input.readBytes().toString(Charsets.UTF_8)
            }
            val root = JsonParser.`object`().from(raw)
            return UrmixBackup.from(root)
        } catch (e: BackupJsonException) {
            throw e
        } catch (e: Exception) {
            throw BackupJsonException("Could not read backup JSON: ${e.message}", e)
        }
    }

    fun preview(backup: UrmixBackup): BackupPreview {
        return BackupPreview(
            likedCount = backup.likedTracks.count { it.hasIdentity() },
            playlistCount = backup.playlists.size,
            historyCount = backup.history.size
        )
    }

    @WorkerThread
    fun apply(context: Context, backup: UrmixBackup): BackupApplyResult {
        val appContext = context.applicationContext
        val database = NewPipeDatabase.getInstance(appContext)
        var liked = 0
        var playlists = 0
        var history = 0
        database.runInTransaction {
            history = restoreHistory(database, backup)
            val restored = restorePlaylists(appContext, database, backup)
            liked = restored.first
            playlists = restored.second
            restoreRemoteBookmarks(database, backup)
            restoreSearchHistory(database, backup)
        }
        database.streamDAO().deleteOrphans()
        return BackupApplyResult(
            likedCount = liked,
            playlistCount = playlists,
            historyCount = history
        )
    }

    internal fun restoreHistory(database: AppDatabase, backup: UrmixBackup): Int {
        val historyTable = database.streamHistoryDAO()
        val streamTable = database.streamDAO()
        historyTable.deleteAll()
        var inserted = 0
        backup.history.take(BackupSchema.MAX_HISTORY_ENTRIES).forEach { entry ->
            val streamEntity = streamEntityFromTrack(entry.track) ?: return@forEach
            val streamId = streamTable.upsert(streamEntity)
            val access = entry.accessDate ?: OffsetDateTime.now()
            historyTable.insert(
                StreamHistoryEntity(
                    streamUid = streamId,
                    accessDate = access,
                    repeatCount = entry.repeatCount
                )
            )
            inserted += 1
        }
        return inserted
    }

    /**
     * Restores the reserved Liked Songs playlist at displayIndex 0, then all
     * regular local playlists starting at displayIndex 1. A stored local
     * playlist with the reserved name is NOT recreated — its tracks are merged
     * into liked_tracks instead (export/import dedup, Option A).
     * @return Pair(likedTrackCount, regularPlaylistCount)
     */
    internal fun restorePlaylists(
        context: Context,
        database: AppDatabase,
        backup: UrmixBackup
    ): Pair<Int, Int> {
        val appContext = context.applicationContext
        val playlistTable = database.playlistDAO()
        val joinTable = database.playlistStreamDAO()
        val streamTable = database.streamDAO()
        joinTable.deleteAll()
        playlistTable.deleteAll()

        val likedName = likedPlaylistName(appContext)
        val likedOnly = backup.likedTracks.filter { it.hasIdentity() }
            .take(BackupSchema.MAX_TRACKS_PER_PLAYLIST)
        val mergedFromPlaylists = backup.playlists
            .filter { !it.isRemote() && it.name == likedName }
            .flatMap { it.tracks }
            .filter { it.hasIdentity() }
        val combinedLiked = (likedOnly + mergedFromPlaylists)
            .distinctBy { it.serviceId to it.url }
            .take(BackupSchema.MAX_TRACKS_PER_PLAYLIST)

        var likedCount = 0
        if (combinedLiked.isNotEmpty()) {
            val likedIds = streamTable.upsertAll(
                combinedLiked.mapNotNull { streamEntityFromTrack(it) }
            )
            val entity = playlistEntityFromBackup(0L, likedName, likedIds)
            val playlistId = playlistTable.insert(entity)
            val joins = likedIds.mapIndexed { joinIndex, streamId ->
                PlaylistStreamEntity(
                    playlistUid = playlistId,
                    streamUid = streamId,
                    index = joinIndex
                )
            }
            if (joins.isNotEmpty()) {
                joinTable.insertAll(joins)
            }
            saveLikedUid(appContext, playlistId)
            likedCount = combinedLiked.size
        } else {
            saveLikedUid(appContext, -1L)
        }

        var index = 1L
        var playlistCount = 0
        backup.playlists.filter { !it.isRemote() }.forEach { playlist ->
            if (playlist.name == likedName) {
                return@forEach
            }
            val validTracks = playlist.tracks.filter { it.hasIdentity() }
                .take(BackupSchema.MAX_TRACKS_PER_PLAYLIST)
            val streamIds = streamTable.upsertAll(
                validTracks.mapNotNull { streamEntityFromTrack(it) }
            )
            val entity = playlistEntityFromBackup(
                index,
                playlist.name.ifEmpty { "Playlist" },
                streamIds
            )
            val playlistId = playlistTable.insert(entity)
            val joins = streamIds.mapIndexed { joinIndex, streamId ->
                PlaylistStreamEntity(
                    playlistUid = playlistId,
                    streamUid = streamId,
                    index = joinIndex
                )
            }
            if (joins.isNotEmpty()) {
                joinTable.insertAll(joins)
            }
            index += 1
            playlistCount += 1
        }
        return likedCount to playlistCount
    }

    internal fun restoreRemoteBookmarks(database: AppDatabase, backup: UrmixBackup) {
        val table = database.playlistRemoteDAO()
        table.deleteAll()
        backup.playlists.filter { it.isRemote() }.forEachIndexed { index, playlist ->
            val url = playlist.url?.takeIf { it.isNotEmpty() } ?: return@forEachIndexed
            table.upsert(
                PlaylistRemoteEntity(
                    serviceId = playlist.serviceId,
                    orderingName = playlist.name,
                    url = url,
                    thumbnailUrl = playlist.thumbnailUrl,
                    uploader = playlist.uploader,
                    displayIndex = index.toLong(),
                    streamCount = playlist.tracks.size.toLong()
                )
            )
        }
    }

    internal fun restoreSearchHistory(database: AppDatabase, backup: UrmixBackup) {
        val table = database.searchHistoryDAO()
        table.deleteAll()
        if (backup.searchHistory.isEmpty()) {
            return
        }
        val entries = backup.searchHistory
            .mapNotNull { it.takeIf { q -> q.isNotEmpty() } }
            .distinct()
            .take(BackupSchema.MAX_SEARCH_ENTRIES)
            .map {
                SearchHistoryEntry(
                    creationDate = OffsetDateTime.now(),
                    serviceId = 0,
                    search = it
                )
            }
        if (entries.isNotEmpty()) {
            table.insertAll(entries)
        }
    }
}
