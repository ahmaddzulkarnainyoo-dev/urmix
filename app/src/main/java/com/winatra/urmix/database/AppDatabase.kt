/*
 * SPDX-FileCopyrightText: 2017-2024 NewPipe contributors <https://newpipe.net>
 * SPDX-FileCopyrightText: 2025 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.winatra.urmix.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.winatra.urmix.database.feed.dao.FeedDAO
import com.winatra.urmix.database.feed.dao.FeedGroupDAO
import com.winatra.urmix.database.feed.model.FeedEntity
import com.winatra.urmix.database.feed.model.FeedGroupEntity
import com.winatra.urmix.database.feed.model.FeedGroupSubscriptionEntity
import com.winatra.urmix.database.feed.model.FeedLastUpdatedEntity
import com.winatra.urmix.database.history.dao.SearchHistoryDAO
import com.winatra.urmix.database.history.dao.StreamHistoryDAO
import com.winatra.urmix.database.history.model.SearchHistoryEntry
import com.winatra.urmix.database.history.model.StreamHistoryEntity
import com.winatra.urmix.database.playlist.dao.PlaylistDAO
import com.winatra.urmix.database.playlist.dao.PlaylistRemoteDAO
import com.winatra.urmix.database.playlist.dao.PlaylistStreamDAO
import com.winatra.urmix.database.playlist.model.PlaylistEntity
import com.winatra.urmix.database.playlist.model.PlaylistRemoteEntity
import com.winatra.urmix.database.playlist.model.PlaylistStreamEntity
import com.winatra.urmix.database.stream.dao.StreamDAO
import com.winatra.urmix.database.stream.dao.StreamStateDAO
import com.winatra.urmix.database.stream.model.StreamEntity
import com.winatra.urmix.database.stream.model.StreamStateEntity
import com.winatra.urmix.database.subscription.SubscriptionDAO
import com.winatra.urmix.database.subscription.SubscriptionEntity

@TypeConverters(Converters::class)
@Database(
    version = Migrations.DB_VER_9,
    entities = [
        SubscriptionEntity::class,
        SearchHistoryEntry::class,
        StreamEntity::class,
        StreamHistoryEntity::class,
        StreamStateEntity::class,
        PlaylistEntity::class,
        PlaylistStreamEntity::class,
        PlaylistRemoteEntity::class,
        FeedEntity::class,
        FeedGroupEntity::class,
        FeedGroupSubscriptionEntity::class,
        FeedLastUpdatedEntity::class
    ]
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun feedDAO(): FeedDAO
    abstract fun feedGroupDAO(): FeedGroupDAO
    abstract fun playlistDAO(): PlaylistDAO
    abstract fun playlistRemoteDAO(): PlaylistRemoteDAO
    abstract fun playlistStreamDAO(): PlaylistStreamDAO
    abstract fun searchHistoryDAO(): SearchHistoryDAO
    abstract fun streamDAO(): StreamDAO
    abstract fun streamHistoryDAO(): StreamHistoryDAO
    abstract fun streamStateDAO(): StreamStateDAO
    abstract fun subscriptionDAO(): SubscriptionDAO

    companion object {
        const val DATABASE_NAME: String = "newpipe.db"
    }
}
