/*
 * SPDX-FileCopyrightText: 2020-2023 NewPipe contributors <https://newpipe.net>
 * SPDX-FileCopyrightText: 2025 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.winatra.urmix.database.playlist

import androidx.room.ColumnInfo
import androidx.room.Embedded
import com.winatra.urmix.database.LocalItem
import com.winatra.urmix.database.playlist.model.PlaylistStreamEntity
import com.winatra.urmix.database.stream.model.StreamEntity
import com.winatra.urmix.database.stream.model.StreamStateEntity
import com.winatra.urmix.util.image.ImageStrategy
import org.schabi.newpipe.extractor.stream.StreamInfoItem

data class PlaylistStreamEntry(
    @Embedded
    val streamEntity: StreamEntity,

    @ColumnInfo(name = StreamStateEntity.STREAM_PROGRESS_MILLIS, defaultValue = "0")
    val progressMillis: Long,

    @ColumnInfo(name = PlaylistStreamEntity.JOIN_STREAM_ID)
    val streamId: Long,

    @ColumnInfo(name = PlaylistStreamEntity.JOIN_INDEX)
    val joinIndex: Int
) : LocalItem {

    override val localItemType: LocalItem.LocalItemType
        get() = LocalItem.LocalItemType.PLAYLIST_STREAM_ITEM

    @Throws(IllegalArgumentException::class)
    fun toStreamInfoItem(): StreamInfoItem {
        return StreamInfoItem(
            streamEntity.serviceId,
            streamEntity.url,
            streamEntity.title,
            streamEntity.streamType
        ).apply {
            duration = streamEntity.duration
            uploaderName = streamEntity.uploader
            uploaderUrl = streamEntity.uploaderUrl
            thumbnails = ImageStrategy.dbUrlToImageList(streamEntity.thumbnailUrl)
        }
    }
}
