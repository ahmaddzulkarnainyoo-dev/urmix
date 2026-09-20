package com.winatra.urmix.home

/*
 * HomeQuickPlayAdapter — URMIX Spotify-style 2-column quick-play grid (blueprint v2 §3.1).
 *
 * Each tile shows rounded artwork + title and forwards taps for audio playback.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.winatra.urmix.databinding.ItemHomeQuickPlayBinding
import com.winatra.urmix.util.image.CoilHelper
import org.schabi.newpipe.extractor.Image

data class QuickPlayTile(
    val title: String,
    val artworkUrl: String?,
    val streamUrl: String?,
    val serviceId: Int,
    val payload: Any? = null
)

class HomeQuickPlayAdapter(
    private val onTileClick: (QuickPlayTile) -> Unit
) : RecyclerView.Adapter<HomeQuickPlayAdapter.TileViewHolder>() {

    private val tiles = ArrayList<QuickPlayTile>()

    fun submitList(newTiles: List<QuickPlayTile>) {
        tiles.clear()
        tiles.addAll(newTiles)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TileViewHolder {
        val binding = ItemHomeQuickPlayBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TileViewHolder(binding, onTileClick)
    }

    override fun onBindViewHolder(holder: TileViewHolder, position: Int) {
        holder.bind(tiles[position])
    }

    override fun getItemCount(): Int = tiles.size

    class TileViewHolder(
        private val binding: ItemHomeQuickPlayBinding,
        private val onTileClick: (QuickPlayTile) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private var current: QuickPlayTile? = null

        init {
            binding.root.setOnClickListener {
                current?.let(onTileClick)
            }
        }

        fun bind(tile: QuickPlayTile) {
            current = tile
            binding.quickPlayTitle.text = tile.title
            val url = tile.artworkUrl
            if (url.isNullOrEmpty()) {
                binding.quickPlayThumbnail.setImageDrawable(null)
            } else {
                CoilHelper.loadThumbnail(
                    binding.quickPlayThumbnail,
                    listOf(
                        Image(
                            url,
                            Image.HEIGHT_UNKNOWN,
                            Image.WIDTH_UNKNOWN,
                            Image.ResolutionLevel.UNKNOWN
                        )
                    )
                )
            }
        }
    }
}
