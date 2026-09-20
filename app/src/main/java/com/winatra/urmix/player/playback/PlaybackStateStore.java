package com.winatra.urmix.player.playback;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

/**
 * Stores the last playback state (current track + playback position) locally in
 * {@link SharedPreferences}, so that playback can be restored after the app is reopened
 * (e.g. after the process was killed).
 */
public final class PlaybackStateStore {
    private static final String PREF_LAST_TRACK_HAS_STATE = "last_track_has_state";
    private static final String PREF_LAST_TRACK_SERVICE_ID = "last_track_service_id";
    private static final String PREF_LAST_TRACK_URL = "last_track_url";
    private static final String PREF_LAST_TRACK_TITLE = "last_track_title";
    private static final String PREF_LAST_TRACK_DURATION = "last_track_duration_seconds";
    private static final String PREF_LAST_TRACK_POSITION = "last_track_position_millis";
    private static final String PREF_LAST_TRACK_SAVED_AT = "last_track_saved_at";

    private PlaybackStateStore() {
        // no instance
    }

    /**
     * Persists the given track and its playback position.
     *
     * @param context         the context
     * @param serviceId       the service id of the track
     * @param url             the url of the track
     * @param title           the title of the track, may be null
     * @param durationSeconds the duration of the track in seconds
     * @param positionMillis  the current playback position in milliseconds
     */
    public static void save(@NonNull final Context context,
                            final int serviceId,
                            @NonNull final String url,
                            @Nullable final String title,
                            final long durationSeconds,
                            final long positionMillis) {
        final SharedPreferences.Editor editor =
                PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(PREF_LAST_TRACK_HAS_STATE, true)
                .putInt(PREF_LAST_TRACK_SERVICE_ID, serviceId)
                .putString(PREF_LAST_TRACK_URL, url)
                .putString(PREF_LAST_TRACK_TITLE, title)
                .putLong(PREF_LAST_TRACK_DURATION, durationSeconds)
                .putLong(PREF_LAST_TRACK_POSITION, positionMillis)
                .putLong(PREF_LAST_TRACK_SAVED_AT, System.currentTimeMillis())
                .apply();
    }

    /**
     * Removes the saved playback state, if any.
     *
     * @param context the context
     */
    public static void clear(@NonNull final Context context) {
        final SharedPreferences.Editor editor =
                PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.remove(PREF_LAST_TRACK_HAS_STATE)
                .remove(PREF_LAST_TRACK_SERVICE_ID)
                .remove(PREF_LAST_TRACK_URL)
                .remove(PREF_LAST_TRACK_TITLE)
                .remove(PREF_LAST_TRACK_DURATION)
                .remove(PREF_LAST_TRACK_POSITION)
                .remove(PREF_LAST_TRACK_SAVED_AT)
                .apply();
    }

    /**
     * @param context the context
     * @return the saved playback state, or null if there is none
     */
    @Nullable
    public static SavedPlaybackState getSavedState(@NonNull final Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (!prefs.getBoolean(PREF_LAST_TRACK_HAS_STATE, false)) {
            return null;
        }
        return new SavedPlaybackState(
                prefs.getInt(PREF_LAST_TRACK_SERVICE_ID, 0),
                prefs.getString(PREF_LAST_TRACK_URL, ""),
                prefs.getString(PREF_LAST_TRACK_TITLE, null),
                prefs.getLong(PREF_LAST_TRACK_DURATION, 0),
                prefs.getLong(PREF_LAST_TRACK_POSITION, 0),
                prefs.getLong(PREF_LAST_TRACK_SAVED_AT, 0));
    }

    /**
     * @param context the context
     * @return whether a playable playback state was saved previously
     */
    public static boolean hasSavedState(@NonNull final Context context) {
        return getSavedState(context) != null;
    }

    /**
     * Immutable snapshot of a previously saved playback state.
     */
    public static final class SavedPlaybackState {
        public final int serviceId;
        @NonNull
        public final String url;
        @Nullable
        public final String title;
        public final long durationSeconds;
        public final long positionMillis;
        public final long savedAt;

        SavedPlaybackState(final int serviceId,
                           @NonNull final String url,
                           @Nullable final String title,
                           final long durationSeconds,
                           final long positionMillis,
                           final long savedAt) {
            this.serviceId = serviceId;
            this.url = url;
            this.title = title;
            this.durationSeconds = durationSeconds;
            this.positionMillis = positionMillis;
            this.savedAt = savedAt;
        }
    }
}

