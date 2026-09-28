package com.winatra.urmix.home

/*
 * HomeFragment — URMIX Spotify-style home (blueprint v2 §3.1 + §3.3).
 * Audio-first reskin on top of NewPipe engine: greeting + quick-play grid +
 * Made For You / Trending Audio / Podcasting carousels.
 * Playback reuses NavigationHelper.playOnMainPlayer(SinglePlayQueue).
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.winatra.urmix.R
import com.winatra.urmix.databinding.FragmentHomeBinding
import com.winatra.urmix.player.playqueue.SinglePlayQueue
import com.winatra.urmix.util.ExtractorHelper
import com.winatra.urmix.util.NavigationHelper
import com.winatra.urmix.util.network.NetworkStateObserver
import com.winatra.urmix.view.OfflineBannerHelper
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.Calendar
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamType

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val disposables = CompositeDisposable()
    private lateinit var madeForYouAdapter: HomeCarouselAdapter
    private lateinit var trendingAdapter: HomeCarouselAdapter
    private lateinit var podcastAdapter: HomeCarouselAdapter

    // FASE 5 §12.2: offline banner + reconnect retry for the cached sections.
    private lateinit var networkObserver: NetworkStateObserver
    private lateinit var offlineBanner: OfflineBannerHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.homeGreeting.text = greetingText()
        madeForYouAdapter = HomeCarouselAdapter(::playStream)
        trendingAdapter = HomeCarouselAdapter(::playStream)
        podcastAdapter = HomeCarouselAdapter(::playStream)
        setupCarousel(binding.homeMadeForYouList, madeForYouAdapter)
        setupCarousel(binding.homeTrendingList, trendingAdapter)
        setupCarousel(binding.homePodcastList, podcastAdapter)
        offlineBanner = OfflineBannerHelper(binding.offlineBanner.root) { reloadContent() }
        networkObserver = NetworkStateObserver(
            requireContext().applicationContext,
            onOnline = { reloadContent() },
            // FASE 5 §12.2: show the banner the moment connectivity is lost, so
            // the cached sections are explained instead of looking broken.
            onOffline = { showOfflineBanner() }
        )
        networkObserver.start()
        binding.homeLoading.visibility = View.VISIBLE
        binding.homeEmpty.visibility = View.GONE
        HomeDataLoader(this, disposables, callbacks()).loadAll()
    }

    override fun onDestroyView() {
        // Guarded: the view can be torn down before the observer/banner were
        // created if view creation failed halfway through.
        if (::networkObserver.isInitialized) {
            networkObserver.stop()
        }
        if (::offlineBanner.isInitialized) {
            offlineBanner.dispose()
        }
        disposables.clear()
        _binding = null
        super.onDestroyView()
    }

    fun showOfflineBanner() {
        if (_binding != null && ::offlineBanner.isInitialized) {
            offlineBanner.show()
        }
    }

    fun hideOfflineBanner() {
        if (_binding != null && ::offlineBanner.isInitialized) {
            offlineBanner.hide()
        }
    }

    fun isOnlineNow(): Boolean = ::networkObserver.isInitialized && networkObserver.isOnline

    private fun reloadContent() {
        hideOfflineBanner()
        if (_binding == null) {
            return
        }
        // Cached sections reload instantly; network sections refresh too so the
        // transition back online is seamless.
        HomeDataLoader(this, disposables, callbacks()).loadAll()
    }

    fun greetingText(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return getString(greetingForHour(hour))
    }

    fun callbacks(): HomeDataLoader.Callbacks = object : HomeDataLoader.Callbacks {
        override fun binding(): FragmentHomeBinding? = _binding
        override fun activityForPlayback() = activity
        override fun appContext() = context?.applicationContext
        override fun madeForYou(): HomeCarouselAdapter = madeForYouAdapter
        override fun trending(): HomeCarouselAdapter = trendingAdapter
        override fun podcast(): HomeCarouselAdapter = podcastAdapter
        override fun play(infoItem: StreamInfoItem) = playStream(infoItem)
    }

    private fun playStream(item: StreamInfoItem) {
        val activity = activity ?: return
        disposables.add(
            ExtractorHelper.getStreamInfo(item.serviceId, item.url, false)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ info ->
                    NavigationHelper.playOnMainPlayer(activity, SinglePlayQueue(info), false)
                }, {
                    NavigationHelper.playOnMainPlayer(activity, SinglePlayQueue(item), false)
                })
        )
    }

    private fun setupCarousel(list: RecyclerView, adapter: HomeCarouselAdapter) {
        list.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        list.adapter = adapter
        list.isNestedScrollingEnabled = false
    }

    companion object {
        @JvmStatic
        fun greetingForHour(hour: Int): Int {
            return when {
                hour < 12 -> R.string.home_greeting_morning
                hour < 18 -> R.string.home_greeting_afternoon
                else -> R.string.home_greeting_evening
            }
        }

        @JvmStatic
        fun isAudioLeaning(type: StreamType?): Boolean {
            return type == StreamType.AUDIO_STREAM ||
                type == StreamType.AUDIO_LIVE_STREAM ||
                type == StreamType.POST_LIVE_AUDIO_STREAM
        }
    }
}
