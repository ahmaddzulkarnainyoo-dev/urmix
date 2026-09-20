package com.winatra.urmix.local.subscription.item

import android.view.View
import com.winatra.urmix.R
import com.winatra.urmix.databinding.FeedGroupAddNewItemBinding
import com.xwray.groupie.viewbinding.BindableItem

class FeedGroupAddNewItem : BindableItem<FeedGroupAddNewItemBinding>() {
    override fun getLayout(): Int = R.layout.feed_group_add_new_item
    override fun initializeViewBinding(view: View) = FeedGroupAddNewItemBinding.bind(view)
    override fun bind(viewBinding: FeedGroupAddNewItemBinding, position: Int) {
        // this is a static item, nothing to do here
    }
}
