package com.winatra.urmix.util.image

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.widget.ImageView
import androidx.annotation.DrawableRes
import coil3.executeBlocking
import coil3.imageLoader
import coil3.request.Disposable
import coil3.request.ImageRequest
import coil3.request.error
import coil3.request.placeholder
import coil3.request.target
import coil3.request.transformations
import coil3.size.Size
import coil3.target.Target
import coil3.toBitmap
import coil3.transform.Transformation
import com.winatra.urmix.MainActivity
import com.winatra.urmix.R
import com.winatra.urmix.ktx.scale
import java.util.WeakHashMap
import kotlin.math.min
import org.schabi.newpipe.extractor.Image

object CoilHelper {
    private val TAG = CoilHelper::class.java.simpleName

    @JvmOverloads
    fun loadBitmapBlocking(
        context: Context,
        url: String?,
        @DrawableRes placeholderResId: Int = 0
    ): Bitmap? = context.imageLoader
        .executeBlocking(getImageRequest(context, url, placeholderResId).build())
        .image
        ?.toBitmap()

    fun loadAvatar(
        target: ImageView,
        images: List<Image>
    ) {
        loadImageDefault(target, images, R.drawable.placeholder_person)
    }

    fun loadAvatar(
        target: ImageView,
        url: String?
    ) {
        loadImageDefault(target, url, R.drawable.placeholder_person)
    }

    fun loadThumbnail(
        target: ImageView,
        images: List<Image>
    ) {
        loadImageDefault(target, images, R.drawable.placeholder_thumbnail_video)
    }

    fun loadThumbnail(
        target: ImageView,
        url: String?
    ) {
        loadImageDefault(target, url, R.drawable.placeholder_thumbnail_video)
    }

    fun loadPlayerThumbnail(
        target: ImageView,
        images: List<Image>,
        @DrawableRes placeholderResId: Int
    ) {
        loadImageDefault(target, images, placeholderResId)
    }

    fun loadScaledDownThumbnail(
        context: Context,
        images: List<Image>,
        target: Target
    ): Disposable {
        val url = ImageStrategy.choosePreferredImage(images)
        val request =
            getImageRequest(context, url, R.drawable.placeholder_thumbnail_video)
                .target(target)
                .transformations(
                    object : Transformation() {
                        override val cacheKey = "COIL_PLAYER_THUMBNAIL_TRANSFORMATION_KEY"

                        override suspend fun transform(
                            input: Bitmap,
                            size: Size
                        ): Bitmap {
                            if (MainActivity.DEBUG) {
                                Log.d(TAG, "Thumbnail - transform() called")
                            }

                            val notificationThumbnailWidth =
                                min(
                                    context.resources.getDimension(R.dimen.player_notification_thumbnail_width),
                                    input.width.toFloat()
                                ).toInt()

                            var newHeight = input.height / (input.width / notificationThumbnailWidth)
                            val result = input.scale(notificationThumbnailWidth, newHeight)

                            return if (result == input || !result.isMutable) {
                                // create a new mutable bitmap to prevent strange crashes on some
                                // devices (see #4638)
                                newHeight = input.height / (input.width / (notificationThumbnailWidth - 1))
                                input.scale(notificationThumbnailWidth, newHeight)
                            } else {
                                result
                            }
                        }
                    }
                ).build()

        return context.imageLoader.enqueue(request)
    }

    fun loadDetailsThumbnail(
        target: ImageView,
        images: List<Image>
    ) {
        val url = ImageStrategy.choosePreferredImage(images)
        loadImageDefault(target, url, R.drawable.placeholder_thumbnail_video, false)
    }

    fun loadBanner(
        target: ImageView,
        images: List<Image>
    ) {
        loadImageDefault(target, images, R.drawable.placeholder_channel_banner)
    }

    fun loadPlaylistThumbnail(
        target: ImageView,
        images: List<Image>
    ) {
        loadImageDefault(target, images, R.drawable.placeholder_thumbnail_playlist)
    }

    fun loadPlaylistThumbnail(
        target: ImageView,
        url: String?
    ) {
        loadImageDefault(target, url, R.drawable.placeholder_thumbnail_playlist)
    }

    private fun loadImageDefault(
        target: ImageView,
        images: List<Image>,
        @DrawableRes placeholderResId: Int
    ) {
        loadImageDefault(target, ImageStrategy.choosePreferredImage(images), placeholderResId)
    }

    private fun loadImageDefault(
        target: ImageView,
        url: String?,
        @DrawableRes placeholderResId: Int,
        showPlaceholder: Boolean = true
    ) {
        val request =
            getImageRequest(target.context, url, placeholderResId, showPlaceholder)
                .target(target)
                .build()
        enqueueTracked(target, request)
    }

    /**
     * Active image requests keyed by their target ImageView (FASE 5 §12.3).
     * A WeakHashMap keeps detached views collectable; enqueueing a new request
     * for the same view cancels the previous one so callbacks never stack up.
     *
     * Main-thread only: every entry is written by [loadImageDefault] and removed
     * by [disposeRequests], both of which are called from view/fragment callbacks
     * (`onBindViewHolder`, `onViewRecycled`, `onDestroyView`). `WeakHashMap` is
     * not thread-safe, so any future background caller must synchronize here.
     */
    private val activeRequests = WeakHashMap<ImageView, Disposable>()

    private fun enqueueTracked(target: ImageView, request: ImageRequest) {
        disposeRequests(target)
        activeRequests[target] = target.context.imageLoader.enqueue(request)
    }

    /**
     * Cancels the pending image request of [target]. Call this from
     * `onViewRecycled`/teardown paths so recycled views stop doing work.
     */
    @JvmStatic
    fun disposeRequests(target: ImageView) {
        activeRequests.remove(target)?.dispose()
    }

    private fun getImageRequest(
        context: Context,
        url: String?,
        @DrawableRes placeholderResId: Int,
        showPlaceholderWhileLoading: Boolean = true
    ): ImageRequest.Builder {
        // if the URL was chosen with `choosePreferredImage` it will be null, but check again
        // `shouldLoadImages` in case the URL was chosen with `imageListToDbUrl` (which is the case
        // for URLs stored in the database)
        val takenUrl = url?.takeIf { it.isNotEmpty() && ImageStrategy.shouldLoadImages() }

        return ImageRequest
            .Builder(context)
            .data(takenUrl)
            .error(placeholderResId)
            .memoryCacheKey(takenUrl)
            .diskCacheKey(takenUrl)
            .apply {
                if (takenUrl != null || showPlaceholderWhileLoading) {
                    placeholder(placeholderResId)
                }
            }
    }
}
