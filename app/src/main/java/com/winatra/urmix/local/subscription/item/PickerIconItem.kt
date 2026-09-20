package com.winatra.urmix.local.subscription.item

import android.view.View
import androidx.annotation.DrawableRes
import com.winatra.urmix.R
import com.winatra.urmix.databinding.PickerIconItemBinding
import com.winatra.urmix.local.subscription.FeedGroupIcon
import com.xwray.groupie.viewbinding.BindableItem

class PickerIconItem(
    val icon: FeedGroupIcon
) : BindableItem<PickerIconItemBinding>() {
    @DrawableRes
    val iconRes: Int = icon.getDrawableRes()

    override fun getLayout(): Int = R.layout.picker_icon_item

    override fun bind(viewBinding: PickerIconItemBinding, position: Int) {
        viewBinding.iconView.setImageResource(iconRes)
    }

    override fun initializeViewBinding(view: View) = PickerIconItemBinding.bind(view)
}
