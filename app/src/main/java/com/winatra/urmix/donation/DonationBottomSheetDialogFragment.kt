package com.winatra.urmix.donation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.winatra.urmix.R
import com.winatra.urmix.remoteconfig.RemoteConfigRepository
import com.winatra.urmix.util.external_communication.ShareUtils

/*
 * DonationBottomSheetDialogFragment — dark-themed support sheet (§6).
 *
 * Content comes from the cached remote config (Saweria link); QRIS uses the
 * bundled local image.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 * This file is part of URMIX, a fork of NewPipe (org.schabi.newpipe).
 */
class DonationBottomSheetDialogFragment : BottomSheetDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.dialog_donation, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = requireContext()
        val donation = runCatching {
            RemoteConfigRepository.getCachedConfig(context)?.donation
        }.getOrNull()

        val titleView: TextView = view.findViewById(R.id.donation_sheet_title)
        val messageView: TextView = view.findViewById(R.id.donation_sheet_message)
        val qrisView: ImageView = view.findViewById(R.id.donation_sheet_qris)
        val saweriaButton: Button = view.findViewById(R.id.donation_sheet_saweria)
        val closeButton: Button = view.findViewById(R.id.donation_sheet_close)

        val title = donation?.title?.takeIf { it.isNotBlank() }
            ?: getString(R.string.donation_notification_title)
        val message = donation?.dailyMessage?.takeIf { it.isNotBlank() }
            ?: getString(R.string.donation_sheet_fallback_message)
        val saweriaUrl = donation?.saweriaUrl ?: getString(R.string.donation_url)

        titleView.text = title
        messageView.text = message
        qrisView.setImageResource(R.drawable.qris_donasi)
        qrisView.isVisible = true
        saweriaButton.setOnClickListener {
            ShareUtils.openUrlInBrowser(context, saweriaUrl)
            dismissAllowingStateLoss()
        }
        closeButton.setOnClickListener { dismissAllowingStateLoss() }
    }

    companion object {
        const val TAG = "DonationBottomSheet"
    }
}
