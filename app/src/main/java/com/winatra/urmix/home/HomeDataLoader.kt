package com.winatra.urmix.home
// URMIX home data loader (blueprint v2 S3.1/S3.3) - part 1 of 4.
import android.content.Context
import android.view.View
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.fragment.app.FragmentActivity
import com.winatra.urmix.NewPipeDatabase
import com.winatra.urmix.R
import com.winatra.urmix.database.feed.model.FeedGroupEntity
import com.winatra.urmix.databinding.FragmentHomeBinding
import com.winatra.urmix.databinding.ItemHomeQuickPlayBinding
import com.winatra.urmix.local.feed.FeedDatabaseManager
import com.winatra.urmix.player.playqueue.SinglePlayQueue
import com.winatra.urmix.util.ExtractorHelper
import com.winatra.urmix.util.NavigationHelper
import com.winatra.urmix.util.image.CoilHelper
import com.winatra.urmix.util.image.ImageStrategy
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.schabi.newpipe.extractor.stream.StreamInfoItem
class HomeDataLoader(
    private val fragment: HomeFragment,
    private val disposables: CompositeDisposable,
    private val callbacks: Callbacks
) {
    interface Callbacks {
        fun binding(): FragmentHomeBinding?
        fun activityForPlayback(): FragmentActivity?
        fun appContext(): Context?
        fun madeForYou(): HomeCarouselAdapter
        fun trending(): HomeCarouselAdapter
        fun podcast(): HomeCarouselAdapter
        fun play(item: StreamInfoItem)
    }
    fun loadAll() {
        loadQuickPlay()
        loadMadeForYou()
        if (fragment.isOnlineNow()) {
            loadTrendingAudio()
            loadPodcasting()
        } else {
            // FASE 5 §12.2: offline — skip the network-only sections, keep the
            // Room-backed sections visible and raise the offline banner.
            fragment.showOfflineBanner()
            updateEmptyState()
        }
    }
    fun loadQuickPlay() {
        val appContext = callbacks.appContext() ?: return
        disposables.add(
            Observable.fromCallable {
                NewPipeDatabase.getInstance(appContext).streamHistoryDAO()
                    .history.blockingFirst()
            }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ entries ->
                    val tiles = entries.take(4).map { entry ->
                        QuickPlayTile(
                            title = entry.streamEntity.title,
                            artworkUrl = entry.streamEntity.thumbnailUrl,
                            streamUrl = entry.streamEntity.url,
                            serviceId = entry.streamEntity.serviceId
                        )
                    }
                    renderQuickPlay(tiles)
                    updateEmptyState()
                }, { updateEmptyState() })
        )
    }
    fun renderQuickPlay(tiles: List<QuickPlayTile>) {
        val binding = callbacks.binding() ?: return
        val grid = binding.homeQuickPlayGrid
        // FASE 5 §12.3: cancel pending artwork requests of the old tiles
        // before dropping them so detached views stop doing work.
        for (i in 0 until grid.childCount) {
            CoilHelper.disposeRequests(grid.getChildAt(i).findViewById(R.id.quick_play_thumbnail))
        }
        grid.removeAllViews()
        if (tiles.isEmpty()) {
            grid.visibility = View.GONE
            return
        }
        grid.visibility = View.VISIBLE
        val context = fragment.requireContext()
        val margin = (8 * context.resources.displayMetrics.density).toInt()
        tiles.forEach { tile ->
            val itemBinding =
                ItemHomeQuickPlayBinding.inflate(fragment.layoutInflater, grid, false)
            itemBinding.quickPlayTitle.text = tile.title
            val art = tile.artworkUrl
            if (art.isNullOrEmpty()) {
                itemBinding.quickPlayThumbnail.setImageDrawable(null)
            } else {
                CoilHelper.loadThumbnail(
                    itemBinding.quickPlayThumbnail,
                    ImageStrategy.dbUrlToImageList(art)
                )
            }
            itemBinding.quickPlayThumbnail.scaleType = ImageView.ScaleType.CENTER_CROP
            itemBinding.root.setOnClickListener { playQuickTile(tile) }
            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(margin, margin, margin, margin)
            }
            val wrapper = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = params
            }
            wrapper.addView(
                itemBinding.root,
                LinearLayout.LayoutParams(-1, -2)
            )
            grid.addView(wrapper)
        }
    }
    fun playQuickTile(tile: QuickPlayTile) {
        val activity = callbacks.activityForPlayback() ?: return
        val url = tile.streamUrl ?: return
        disposables.add(
            ExtractorHelper.getStreamInfo(tile.serviceId, url, false)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ info ->
                    NavigationHelper.playOnMainPlayer(activity, SinglePlayQueue(info), false)
                }, { })
        )
    }
    fun loadMadeForYou() {
        val appContext = callbacks.appContext() ?: return
        val manager = FeedDatabaseManager(appContext)
        disposables.add(
            manager.getStreams(
                FeedGroupEntity.GROUP_ALL_ID,
                true,
                true,
                true
            )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ streams ->
                    val items = streams.take(20).mapNotNull { withState ->
                        val stream = withState.stream
                        if (stream.url.isEmpty()) {
                            return@mapNotNull null
                        }
                        StreamInfoItem(
                            stream.serviceId,
                            stream.url,
                            stream.title,
                            stream.streamType
                        ).apply {
                            uploaderName = stream.uploader
                            thumbnails = ImageStrategy.dbUrlToImageList(stream.thumbnailUrl)
                        }
                    }
                    callbacks.madeForYou().submitList(items)
                    updateEmptyState()
                }, { updateEmptyState() })
        )
    }
    fun loadTrendingAudio() {
        val appContext = callbacks.appContext() ?: return
        disposables.add(
            Observable.fromCallable {
                val serviceId = com.winatra.urmix.util.ServiceHelper
                    .getSelectedServiceId(appContext)
                val service = org.schabi.newpipe.extractor.NewPipe.getService(serviceId)
                val kioskList = service.kioskList
                val kioskId = kioskList.defaultKioskId
                val url = kioskList.getListLinkHandlerFactoryByType(kioskId)
                    .fromId(kioskId).url
                android.util.Pair(serviceId, url)
            }
                .subscribeOn(Schedulers.io())
                .flatMapSingle { pair ->
                    ExtractorHelper.getKioskInfo(pair.first, pair.second, false)
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ kiosk ->
                    // v1.0.2: kiosk kosong → panggil fallback podcast-channel supaya tidak kosong.
                    if (kiosk.relatedItems.isEmpty()) {
                        loadPodcastingChannelFallback(appContext)
                        return@subscribe
                    }
                    val items = kiosk.relatedItems
                        .filterIsInstance<StreamInfoItem>()
                        .sortedWith(
                            compareBy(
                                { !HomeFragment.isAudioLeaning(it.streamType) },
                                { it.name.lowercase() }
                            )
                        ).take(20)
                    callbacks.trending().submitList(items)
                    updateEmptyState()
                }, { updateEmptyState() })
        )
    }
    fun loadPodcasting() {
        val appContext = callbacks.appContext() ?: return
        val cached = runCatching {
            com.winatra.urmix.remoteconfig.RemoteConfigRepository
                .getCachedConfig(appContext)?.podcastSources
        }.getOrNull().orEmpty()
        val sources = (
            cached + listOf(
                "https://www.youtube.com/@TED",
                "https://www.youtube.com/@lexfridman"
            )
            ).distinct().take(6)
        if (sources.isEmpty()) {
            updateEmptyState()
            return
        }
        disposables.add(
            Observable.fromIterable(sources)
                .concatMapSingle { url ->
                    resolvePodcastSource(appContext, url)
                        .subscribeOn(Schedulers.io())
                        .onErrorReturnItem(emptyList())
                }
                .toList()
                // v1.0.2: cap 10 item per carousel supaya tidak didominasi satu section.
                .map { pages -> pages.flatten().take(10) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ items ->
                    callbacks.podcast().submitList(items)
                    val binding = callbacks.binding()
                    if (binding != null) {
                        val visible = if (items.isEmpty()) View.GONE else View.VISIBLE
                        binding.homePodcastTitle.visibility = visible
                        binding.homePodcastList.visibility = visible
                    }
                    updateEmptyState()
                }, { updateEmptyState() })
        )
    }

    // v1.0.2: fallback ketika kiosk trending kosong — pinjam 10 item pertama dari
    // section podcast (diobserve di main thread) supaya section tidak kosong.
    fun loadPodcastingChannelFallback(appContext: Context) {
        disposables.add(
            NewPipeDatabase.getInstance(appContext).streamHistoryDAO()
                .history.take(1)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ _ ->
                    loadPodcasting()
                    updateEmptyState()
                }, { updateEmptyState() })
        )
    }

    fun resolvePodcastSource(
        appContext: Context,
        url: String
    ): io.reactivex.rxjava3.core.Single<List<StreamInfoItem>> {
        return io.reactivex.rxjava3.core.Single.fromCallable {
            val serviceId = runCatching {
                org.schabi.newpipe.extractor.NewPipe.getServiceByUrl(url).serviceId
            }.getOrDefault(
                com.winatra.urmix.util.ServiceHelper.getSelectedServiceId(appContext)
            )
            val service = org.schabi.newpipe.extractor.NewPipe.getService(serviceId)
            Triple(serviceId, url, service.getLinkTypeByUrl(url))
        }.flatMap { parts ->
            val serviceId = parts.first
            val sourceUrl = parts.second
            val linkType = parts.third
            when (linkType) {
                org.schabi.newpipe.extractor.StreamingService.LinkType.STREAM ->
                    ExtractorHelper.getStreamInfo(serviceId, sourceUrl, false)
                        .map { info ->
                            listOf(
                                StreamInfoItem(
                                    info.serviceId,
                                    info.originalUrl,
                                    info.name,
                                    info.streamType
                                ).apply {
                                    uploaderName = info.uploaderName
                                    thumbnails = info.thumbnails
                                }
                            )
                        }

                org.schabi.newpipe.extractor.StreamingService.LinkType.CHANNEL ->
                    ExtractorHelper.getChannelInfo(serviceId, sourceUrl, false)
                        .flatMap { info ->
                            val tab = info.tabs.firstOrNull()
                            if (tab == null) {
                                io.reactivex.rxjava3.core.Single.just(emptyList())
                            } else {
                                ExtractorHelper.getChannelTab(
                                    serviceId,
                                    tab,
                                    false
                                ).map { page ->
                                    page.relatedItems
                                        .filterIsInstance<StreamInfoItem>().take(8)
                                }
                            }
                        }

                else ->
                    ExtractorHelper.getPlaylistInfo(serviceId, sourceUrl, false)
                        .map { info ->
                            info.relatedItems
                                .filterIsInstance<StreamInfoItem>().take(8)
                        }
            }
        }
    }
    fun updateEmptyState() {
        val binding = callbacks.binding() ?: return
        binding.homeLoading.visibility = View.GONE
        val hasContent = callbacks.madeForYou().itemCount > 0 ||
            callbacks.trending().itemCount > 0 ||
            callbacks.podcast().itemCount > 0 ||
            binding.homeQuickPlayGrid.childCount > 0
        binding.homeEmpty.visibility = if (hasContent) View.GONE else View.VISIBLE
    }
}
