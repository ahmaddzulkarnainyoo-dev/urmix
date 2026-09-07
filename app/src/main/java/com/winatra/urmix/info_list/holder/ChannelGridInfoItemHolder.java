package com.winatra.urmix.info_list.holder;

import android.view.ViewGroup;

import com.winatra.urmix.R;
import com.winatra.urmix.info_list.InfoItemBuilder;

public class ChannelGridInfoItemHolder extends ChannelMiniInfoItemHolder {
    public ChannelGridInfoItemHolder(final InfoItemBuilder infoItemBuilder,
                                     final ViewGroup parent) {
        super(infoItemBuilder, R.layout.list_channel_grid_item, parent);
    }
}
