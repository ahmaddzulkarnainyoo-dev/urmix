package com.winatra.urmix.player.resolver;

import static com.winatra.urmix.util.ListHelper.getFilteredAudioStreams;
import static com.winatra.urmix.util.ListHelper.getPlayableStreams;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.source.MediaSource;

import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.Stream;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.VideoStream;
import com.winatra.urmix.player.helper.PlayerDataSource;
import com.winatra.urmix.player.mediaitem.MediaItemTag;
import com.winatra.urmix.player.mediaitem.StreamInfoTag;
import com.winatra.urmix.util.ListHelper;

import java.util.List;

public class AudioPlaybackResolver implements PlaybackResolver {
    private static final String TAG = AudioPlaybackResolver.class.getSimpleName();

    @NonNull
    private final Context context;
    @NonNull
    private final PlayerDataSource dataSource;
    @Nullable
    private String audioTrack;

    public AudioPlaybackResolver(@NonNull final Context context,
                                 @NonNull final PlayerDataSource dataSource) {
        this.context = context;
        this.dataSource = dataSource;
    }

    /**
     * Get a media source providing audio. If a service has no separate {@link AudioStream}s we
     * use a video stream as audio source to support audio background playback.
     *
     * @param info of the stream
     * @return the audio source to use or null if none could be found
     */
    @Override
    @Nullable
    public MediaSource resolve(@NonNull final StreamInfo info) {
        final MediaSource liveSource = PlaybackResolver.maybeBuildLiveMediaSource(dataSource, info);
        if (liveSource != null) {
            return liveSource;
        }

        final List<AudioStream> audioStreams =
                getFilteredAudioStreams(context, info.getAudioStreams());
        final Stream stream;
        final MediaItemTag tag;

        if (!audioStreams.isEmpty()) {
            final int audioIndex =
                    ListHelper.getAudioFormatIndex(context, audioStreams, audioTrack);
            stream = getStreamForIndex(audioIndex, audioStreams);
            tag = StreamInfoTag.of(info, audioStreams, audioIndex);
        } else {
            // Fallback (§4.2): no audio-only stream is available for this track, so fall back to
            // the standard video stream and play it as audio only. Pick the lowest resolution to
            // save bandwidth, since the video track is never rendered in the audio player
            // (video rendering is disabled by the player UI).
            final List<VideoStream> videoStreams =
                    getPlayableStreams(info.getVideoStreams(), info.getServiceId());
            if (!videoStreams.isEmpty()) {
                final List<VideoStream> sortedAscending = ListHelper.getSortedStreamVideosList(
                        context, videoStreams, null, /*ascendingOrder=*/true,
                        /*preferVideoOnlyStreams=*/false);
                final int index = 0; // lowest resolution
                stream = getStreamForIndex(index, sortedAscending);
                tag = StreamInfoTag.of(info);
                Log.d(TAG, "No audio-only stream available for [" + info.getName()
                        + "], falling back to " + sortedAscending.size()
                        + " video stream(s) played as audio only");
            } else {
                return null;
            }
        }

        try {
            return PlaybackResolver.buildMediaSource(
                    dataSource, stream, info, PlaybackResolver.cacheKeyOf(info, stream), tag);
        } catch (final ResolverException e) {
            Log.e(TAG, "Unable to create audio source", e);
            return null;
        }
    }

    @Nullable
    Stream getStreamForIndex(final int index, @NonNull final List<? extends Stream> streams) {
        if (index >= 0 && index < streams.size()) {
            return streams.get(index);
        }
        return null;
    }

    @Nullable
    public String getAudioTrack() {
        return audioTrack;
    }

    public void setAudioTrack(@Nullable final String audioLanguage) {
        this.audioTrack = audioLanguage;
    }
}
