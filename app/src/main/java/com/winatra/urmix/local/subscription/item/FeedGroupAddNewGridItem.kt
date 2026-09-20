package com.winatra.urmix.local.subscription.item

import android.view.View
import com.winatra.urmix.R
import com.winatra.urmix.databinding.FeedGroupAddNewGridItemBinding
import com.xwray.groupie.viewbinding.BindableItem

class FeedGroupAddNewGridItem : BindableItem<FeedGroupAddNewGridItemBinding>() {
    override fun getLayout(): Int = R.layout.feed_group_add_new_grid_item
    override fun initializeViewBinding(view: View) = FeedGroupAddNewGridItemBinding.bind(view)
    override fun bind(viewBinding: FeedGroupAddNewGridItemBinding, position: Int) {
        // this is a static item, nothing to do here
    }
}
