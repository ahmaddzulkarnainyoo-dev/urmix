package com.winatra.urmix.local.playlist;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.winatra.urmix.NewPipeDatabase;
import com.winatra.urmix.R;
import com.winatra.urmix.database.AppDatabase;
import com.winatra.urmix.database.playlist.PlaylistDuplicatesEntry;
import com.winatra.urmix.database.playlist.PlaylistMetadataEntry;
import com.winatra.urmix.database.playlist.model.PlaylistEntity;
import com.winatra.urmix.database.stream.model.StreamEntity;
import com.winatra.urmix.settings.export.LibraryBackupManagerKt;

import org.schabi.newpipe.extractor.stream.StreamInfo;

import io.reactivex.rxjava3.core.Maybe;

import java.util.ArrayList;
import java.util.List;

/**
 * URMIX Phase C — Liked Songs manager (blueprint v2 §5.1).
 *
 * <p>The reserved "Liked Songs" playlist is identified by the UID stored in
 * {@code urmix_backup_prefs}, with a self-healing fallback to lookup by playlist
 * name (see {@code LibraryBackupManagerKt.resolveLikedUid}). All methods that
 * touch the database are blocking and must run on a worker thread.</p>
 */
public final class LikedSongsManager {
    private static final String TAG = "LikedSongsManager";

    // Serializes like/unlike toggles so two rapid taps cannot create
    // duplicate "Liked Songs" playlists (both chains could otherwise see
    // likedUid < 0 and each create the reserved playlist).
    private static final Object TOGGLE_LOCK = new Object();

    private LikedSongsManager() {
    }

    @NonNull
    public static String likedPlaylistName(@NonNull final Context context) {
        return context.getString(R.string.liked_songs_playlist_name);
    }

    /**
     * Resolve the reserved Liked Songs playlist UID (self-healing, mirrors export/import).
     *
     * @return playlist UID or -1 when there is no Liked Songs playlist yet
     */
    @WorkerThread
    public static long resolveLikedPlaylistId(@NonNull final Context context) {
        try {
            final Context appContext = context.getApplicationContext();
            return LibraryBackupManagerKt.resolveLikedUid(
                    appContext, NewPipeDatabase.getInstance(appContext));
        } catch (final Exception e) {
            Log.w(TAG, "Could not resolve Liked Songs playlist", e);
            return -1L;
        }
    }

    /**
     * Check whether the given stream URL is already in the Liked Songs playlist.
     *
     * @return true when liked, false otherwise (or when Liked Songs does not exist / on error)
     */
    @WorkerThread
    public static boolean isLiked(@NonNull final Context context,
                                  @Nullable final String streamUrl) {
        if (streamUrl == null || streamUrl.isEmpty()) {
            return false;
        }
        try {
            final Context appContext = context.getApplicationContext();
            final AppDatabase database = NewPipeDatabase.getInstance(appContext);
            final long likedUid = LibraryBackupManagerKt.resolveLikedUid(
                    appContext, database);
            if (likedUid < 0) {
                return false;
            }
            final List<PlaylistDuplicatesEntry> duplicates =
                    new LocalPlaylistManager(database)
                            .getPlaylistDuplicates(streamUrl)
                            .blockingFirst(new ArrayList<>());
            for (final PlaylistDuplicatesEntry entry : duplicates) {
                if (entry.getUid() == likedUid && entry.getTimesStreamIsContained() > 0) {
                    return true;
                }
            }
            return false;
        } catch (final Exception e) {
            Log.w(TAG, "Could not check liked state", e);
            return false;
        }
    }

    /**
     * Toggle the given stream in the Liked Songs playlist.
     *
     * <p>When the stream is not liked yet, it is appended (creating the reserved
     * playlist when needed and persisting its UID). When it is already liked,
     * its join row is removed; orphaned stream rows are cleaned up.</p>
     *
     * @return a Maybe emitting the new liked state (true = now liked);
     *         empty when the toggle failed
     */
    @WorkerThread
    @NonNull
    public static Maybe<Boolean> toggleMaybe(@NonNull final Context context,
                                             @NonNull final StreamInfo info) {
        if (info.getUrl() == null || info.getUrl().isEmpty()) {
            return Maybe.empty();
        }
        return Maybe.fromCallable(() -> toggleBlocking(context, info));
    }

    @WorkerThread
    @Nullable
    static Boolean toggleBlocking(@NonNull final Context context,
                                  @NonNull final StreamInfo info) {
        if (info.getUrl() == null || info.getUrl().isEmpty()) {
            return null;
        }
        synchronized (TOGGLE_LOCK) {
            return toggleLocked(context, info);
        }
    }

    @WorkerThread
    @Nullable
    private static Boolean toggleLocked(@NonNull final Context context,
                                        @NonNull final StreamInfo info) {
        if (info.getUrl() == null || info.getUrl().isEmpty()) {
            return null;
        }
        try {
            final Context appContext = context.getApplicationContext();
            final AppDatabase database = NewPipeDatabase.getInstance(appContext);
            final LocalPlaylistManager manager = new LocalPlaylistManager(database);
            final long likedUid = LibraryBackupManagerKt.resolveLikedUid(
                    appContext, database);
            final String streamUrl = info.getUrl();

            if (isLikedInternal(manager, likedUid, streamUrl)) {
                // Unlike: remove only this stream's join row from Liked Songs.
                final List<StreamEntity> rows = database.streamDAO()
                        .getStream((long) info.getServiceId(), streamUrl)
                        .blockingFirst(new ArrayList<>());
                if (!rows.isEmpty()) {
                    final long streamId = rows.get(0).getUid();
                    final long targetUid = likedUid;
                    database.runInTransaction((Runnable) () ->
                            database.playlistStreamDAO()
                                    .deleteByStreamId(targetUid, streamId));
                    database.streamDAO().deleteOrphans();
                }
                return false;
            }

            final StreamEntity entity = new StreamEntity(info);
            if (likedUid < 0) {
                // First like ever: create the reserved playlist.
                final List<Long> joinIds = manager
                        .createPlaylist(likedPlaylistName(appContext), List.of(entity))
                        .blockingGet();
                if (joinIds == null || joinIds.isEmpty()) {
                    return null;
                }
                final List<PlaylistMetadataEntry> metas =
                        LibraryBackupManagerKt.defaultPlaylistsOf(database);
                long createdUid = -1L;
                for (final PlaylistMetadataEntry meta : metas) {
                    if (likedPlaylistName(appContext).equals(meta.getOrderingName())) {
                        createdUid = meta.getUid();
                        break;
                    }
                }
                if (createdUid < 0) {
                    return null;
                }
                LibraryBackupManagerKt.saveLikedUid(appContext, createdUid);
                movePlaylistToTop(database, createdUid);
                return true;
            }

            final List<Long> appended = manager
                    .appendToPlaylist(likedUid, List.of(entity))
                    .blockingGet();
            return appended != null ? true : null;
        } catch (final Exception e) {
            Log.w(TAG, "Could not toggle liked state", e);
            return null;
        }
    }

    private static boolean isLikedInternal(final LocalPlaylistManager manager,
                                           final long likedUid,
                                           final String streamUrl) {
        if (likedUid < 0 || streamUrl == null || streamUrl.isEmpty()) {
            return false;
        }
        final List<PlaylistDuplicatesEntry> duplicates = manager
                .getPlaylistDuplicates(streamUrl)
                .blockingFirst(new ArrayList<>());
        for (final PlaylistDuplicatesEntry entry : duplicates) {
            if (entry.getUid() == likedUid && entry.getTimesStreamIsContained() > 0) {
                return true;
            }
        }
        return false;
    }

    private static void movePlaylistToTop(final AppDatabase database,
                                          final long playlistId) {
        try {
            final List<PlaylistEntity> rows = database.playlistDAO()
                    .getPlaylist(playlistId)
                    .blockingFirst(new ArrayList<>());
            if (rows.isEmpty()) {
                return;
            }
            final PlaylistEntity entity = rows.get(0);
            entity.setDisplayIndex(0);
            database.runInTransaction((Runnable) () ->
                    database.playlistDAO().upsertPlaylist(entity));
        } catch (final Exception e) {
            Log.w(TAG, "Could not move Liked Songs to top", e);
        }
    }
}
