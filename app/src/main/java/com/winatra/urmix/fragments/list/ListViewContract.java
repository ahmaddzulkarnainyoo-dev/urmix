package com.winatra.urmix.fragments.list;

import com.winatra.urmix.fragments.ViewContract;

public interface ListViewContract<I, N> extends ViewContract<I> {
    void showListFooter(boolean show);

    void handleNextItems(N result);
}
