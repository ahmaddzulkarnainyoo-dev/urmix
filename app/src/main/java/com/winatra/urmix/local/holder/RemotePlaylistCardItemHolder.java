package com.winatra.urmix.local.holder;

import android.view.ViewGroup;

import com.winatra.urmix.R;
import com.winatra.urmix.local.LocalItemBuilder;

/**
 * Playlist card UI for list item.
 */
public class RemotePlaylistCardItemHolder extends RemotePlaylistItemHolder {

    public RemotePlaylistCardItemHolder(final LocalItemBuilder infoItemBuilder,
                                        final ViewGroup parent) {
        super(infoItemBuilder, R.layout.list_playlist_card_item, parent);
    }
}
