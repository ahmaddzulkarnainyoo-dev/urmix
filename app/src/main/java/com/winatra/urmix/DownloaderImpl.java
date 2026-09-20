package com.winatra.urmix;

import android.app.ActivityManager;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.winatra.urmix.error.ReCaptchaActivity;
import com.winatra.urmix.network.HttpCacheControlInterceptor;
import com.winatra.urmix.network.OfflineCacheInterceptor;
import com.winatra.urmix.util.CacheConfig;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Request;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException;
import com.winatra.urmix.util.InfoCache;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;

public final class DownloaderImpl extends Downloader {
    public static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0";
    public static final String YOUTUBE_RESTRICTED_MODE_COOKIE_KEY =
            "youtube_restricted_mode_key";
    public static final String YOUTUBE_RESTRICTED_MODE_COOKIE = "PREF=f2=8000000";
    public static final String YOUTUBE_DOMAIN = "youtube.com";

    private static DownloaderImpl instance;
    private final Map<String, String> mCookies;
    private final OkHttpClient client;

    private DownloaderImpl(final OkHttpClient.Builder builder, @Nullable final Context context) {
        final OkHttpClient.Builder finalBuilder = builder.readTimeout(30, TimeUnit.SECONDS);
        if (context != null) {
            // FASE 5 §12.1: bounded shared HTTP response cache + offline fallback.
            // Only URMIX-owned hosts are cached (see HostCachePolicy); extractor
            // traffic is pinned to `no-store` so stale stream URLs are impossible.
            final File externalCacheDir = context.getExternalCacheDir();
            final File cacheDir = new File(
                    externalCacheDir != null ? externalCacheDir : context.getCacheDir(),
                    CacheConfig.HTTP_CACHE_DIR);
            finalBuilder
                    .cache(new Cache(cacheDir, CacheConfig.httpCacheBytes(isLowRamDevice(context))))
                    .addInterceptor(new OfflineCacheInterceptor(context))
                    .addNetworkInterceptor(new HttpCacheControlInterceptor());
        }
        this.client = finalBuilder.build();
        this.mCookies = new HashMap<>();
    }

    private static boolean isLowRamDevice(@NonNull final Context context) {
        final ActivityManager activityManager =
                ContextCompat.getSystemService(context, ActivityManager.class);
        return activityManager != null && activityManager.isLowRamDevice();
    }

    @NonNull
    public OkHttpClient getClient() {
        return client;
    }

    /**
     * It's recommended to call exactly once in the entire lifetime of the application.
     *
     * @param builder if null, default builder will be used
     * @return a new instance of {@link DownloaderImpl}
     */
    public static DownloaderImpl init(@Nullable final OkHttpClient.Builder builder) {
        return init(null, builder);
    }

    /**
     * Like {@link #init(OkHttpClient.Builder)}, but additionally installs the
     * FASE 5 §12.1 shared HTTP response cache when a context is provided.
     *
     * @param context app context, may be null to skip the HTTP cache entirely
     * @param builder if null, default builder will be used
     * @return a new instance of {@link DownloaderImpl}
     */
    public static DownloaderImpl init(@Nullable final Context context,
                                      @Nullable final OkHttpClient.Builder builder) {
        instance = new DownloaderImpl(
                builder != null ? builder : new OkHttpClient.Builder(), context);
        return instance;
    }

    public static DownloaderImpl getInstance() {
        return instance;
    }

    public String getCookies(final String url) {
        final String youtubeCookie = url.contains(YOUTUBE_DOMAIN)
                ? getCookie(YOUTUBE_RESTRICTED_MODE_COOKIE_KEY) : null;

        // Recaptcha cookie is always added TODO: not sure if this is necessary
        return Stream.of(youtubeCookie, getCookie(ReCaptchaActivity.RECAPTCHA_COOKIES_KEY))
                .filter(Objects::nonNull)
                .flatMap(cookies -> Arrays.stream(cookies.split("; *")))
                .distinct()
                .collect(Collectors.joining("; "));
    }

    public String getCookie(final String key) {
        return mCookies.get(key);
    }

    public void setCookie(final String key, final String cookie) {
        mCookies.put(key, cookie);
    }

    public void removeCookie(final String key) {
        mCookies.remove(key);
    }

    public void updateYoutubeRestrictedModeCookies(final Context context) {
        final String restrictedModeEnabledKey =
                context.getString(R.string.youtube_restricted_mode_enabled);
        final boolean restrictedModeEnabled = PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(restrictedModeEnabledKey, false);
        updateYoutubeRestrictedModeCookies(restrictedModeEnabled);
    }

    public void updateYoutubeRestrictedModeCookies(final boolean youtubeRestrictedModeEnabled) {
        if (youtubeRestrictedModeEnabled) {
            setCookie(YOUTUBE_RESTRICTED_MODE_COOKIE_KEY,
                    YOUTUBE_RESTRICTED_MODE_COOKIE);
        } else {
            removeCookie(YOUTUBE_RESTRICTED_MODE_COOKIE_KEY);
        }
        InfoCache.getInstance().clearCache();
    }

    /**
     * Get the size of the content that the url is pointing by firing a HEAD request.
     *
     * @param url an url pointing to the content
     * @return the size of the content, in bytes
     */
    public long getContentLength(final String url) throws IOException {
        try {
            final Response response = head(url);
            return Long.parseLong(response.getHeader("Content-Length"));
        } catch (final NumberFormatException e) {
            throw new IOException("Invalid content length", e);
        } catch (final ReCaptchaException e) {
            throw new IOException(e);
        }
    }

    /**
     * Clears the shared HTTP response cache installed by
     * {@link #init(Context, OkHttpClient.Builder)} (FASE 5 §12.1).
     *
     * @return true when a cache was installed and successfully cleared
     */
    public boolean clearHttpCache() {
        final Cache cache = client.cache();
        if (cache == null) {
            return false;
        }
        try {
            cache.evictAll();
            return true;
        } catch (final IOException e) {
            return false;
        }
    }

    /**
     * @return the size of the shared HTTP response cache in bytes,
     * or {@code -1L} when no cache is installed or the size cannot be read
     */
    public long getHttpCacheSizeBytes() {
        final Cache cache = client.cache();
        if (cache == null) {
            return -1L;
        }
        try {
            return cache.size();
        } catch (final IOException e) {
            return -1L;
        }
    }

    @Override
    public Response execute(@NonNull final Request request)
            throws IOException, ReCaptchaException {
        final String httpMethod = request.httpMethod();
        final String url = request.url();
        final Map<String, List<String>> headers = request.headers();
        final byte[] dataToSend = request.dataToSend();

        RequestBody requestBody = null;
        if (dataToSend != null) {
            requestBody = RequestBody.create(dataToSend);
        }

        final okhttp3.Request.Builder requestBuilder = new okhttp3.Request.Builder()
                .method(httpMethod, requestBody)
                .url(url)
                .addHeader("User-Agent", USER_AGENT);

        final String cookies = getCookies(url);
        if (!cookies.isEmpty()) {
            requestBuilder.addHeader("Cookie", cookies);
        }

        headers.forEach((headerName, headerValueList) -> {
            requestBuilder.removeHeader(headerName);
            headerValueList.forEach(headerValue ->
                    requestBuilder.addHeader(headerName, headerValue));
        });

        try (
                okhttp3.Response response = client.newCall(requestBuilder.build()).execute()
        ) {
            if (response.code() == 429) {
                throw new ReCaptchaException("reCaptcha Challenge requested", url);
            }

            String responseBodyToReturn = null;
            try (ResponseBody body = response.body()) {
                responseBodyToReturn = body.string();
            }

            final String latestUrl = response.request().url().toString();
            return new Response(
                    response.code(),
                    response.message(),
                    response.headers().toMultimap(),
                    responseBodyToReturn,
                    latestUrl);
        }
    }
}
