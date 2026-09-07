package com.winatra.urmix.local.holder;

import android.text.TextUtils;
import android.view.ViewGroup;

import com.winatra.urmix.database.LocalItem;
import com.winatra.urmix.database.playlist.model.PlaylistRemoteEntity;
import com.winatra.urmix.local.LocalItemBuilder;
import com.winatra.urmix.local.history.HistoryRecordManager;
import com.winatra.urmix.util.Localization;
import com.winatra.urmix.util.ServiceHelper;
import com.winatra.urmix.util.image.CoilHelper;

import java.time.format.DateTimeFormatter;

public class RemotePlaylistItemHolder extends PlaylistItemHolder {

    public RemotePlaylistItemHolder(final LocalItemBuilder infoItemBuilder,
                                    final ViewGroup parent) {
        super(infoItemBuilder, parent);
    }

    RemotePlaylistItemHolder(final LocalItemBuilder infoItemBuilder, final int layoutId,
                             final ViewGroup parent) {
        super(infoItemBuilder, layoutId, parent);
    }

    @Override
    public void updateFromItem(final LocalItem localItem,
                               final HistoryRecordManager historyRecordManager,
                               final DateTimeFormatter dateTimeFormatter) {
        if (!(localItem instanceof PlaylistRemoteEntity item)) {
            return;
        }

        itemTitleView.setText(item.getOrderingName());
        itemStreamCountView.setText(Localization.localizeStreamCountMini(
                itemStreamCountView.getContext(), item.getStreamCount()));
        // Here is where the uploader name is set in the bookmarked playlists library
        if (!TextUtils.isEmpty(item.getUploader())) {
            itemUploaderView.setText(Localization.concatenateStrings(item.getUploader(),
                    ServiceHelper.getNameOfServiceById(item.getServiceId())));
        } else {
            itemUploaderView.setText(ServiceHelper.getNameOfServiceById(item.getServiceId()));
        }

        CoilHelper.INSTANCE.loadPlaylistThumbnail(itemThumbnailView, item.getThumbnailUrl());

        super.updateFromItem(localItem, historyRecordManager, dateTimeFormatter);
    }
}
