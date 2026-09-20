package com.winatra.urmix.home

/*
 * HomeCarouselAdapter — URMIX Spotify-style horizontal track cards (blueprint v2 §3.1/§3.3).
 *
 * Binds StreamInfoItem data (title, uploader, artwork) into item_home_track_card.xml
 * and forwards taps to the caller so fragments can start playback via the existing
 * NavigationHelper.playOnMainPlayer(SinglePlayQueue) path.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.winatra.urmix.databinding.ItemHomeTrackCardBinding
import com.winatra.urmix.util.image.CoilHelper
import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.stream.StreamInfoItem

class HomeCarouselAdapter(
    private val onItemClick: (StreamInfoItem) -> Unit
) : RecyclerView.Adapter<HomeCarouselAdapter.TrackViewHolder>() {

    private val items = ArrayList<StreamInfoItem>()

    fun submitList(newItems: List<StreamInfoItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val binding = ItemHomeTrackCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TrackViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun onViewRecycled(holder: TrackViewHolder) {
        super.onViewRecycled(holder)
        holder.onViewRecycled()
    }

    override fun getItemCount(): Int = items.size

    class TrackViewHolder(
        private val binding: ItemHomeTrackCardBinding,
        private val onItemClick: (StreamInfoItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private var current: StreamInfoItem? = null

        init {
            binding.root.setOnClickListener {
                current?.let(onItemClick)
            }
        }

        fun bind(item: StreamInfoItem) {
            current = item
            binding.trackCardTitle.text = item.name
            val subtitle = item.uploaderName?.takeIf { it.isNotEmpty() }
                ?: item.uploaderUrl?.takeIf { it.isNotEmpty() }
                ?: ""
            binding.trackCardSubtitle.text = subtitle
            loadArt(binding.trackCardThumbnail, item.thumbnails)
        }

        /** Cancels pending artwork requests (FASE 5 §12.3). */
        fun onViewRecycled() {
            CoilHelper.disposeRequests(binding.trackCardThumbnail)
        }

        private fun loadArt(target: ImageView, images: List<Image>?) {
            if (images.isNullOrEmpty()) {
                target.setImageDrawable(null)
            } else {
                CoilHelper.loadThumbnail(target, images)
            }
        }
    }
}
