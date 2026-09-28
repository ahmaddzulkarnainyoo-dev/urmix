package com.winatra.urmix.view

import android.content.Context
import android.content.SharedPreferences
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.content.edit
import androidx.core.view.isVisible
import com.winatra.urmix.R
import com.winatra.urmix.ktx.animate
import com.winatra.urmix.remoteconfig.RemoteConfigRepository
import com.winatra.urmix.remoteconfig.UpdateGate
import com.winatra.urmix.util.external_communication.ShareUtils

/*
 * SoftUpdateBannerHelper — dismissible non-blocking update notice (FASE 6 §7.3).
 *
 * Shown only when RemoteConfigRepository.getUpdateState() reports SOFT_UPDATE.
 * Dismissal is persisted per required app version so the banner does not nag
 * again for the same release, but reappears for newer ones.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 * This file is part of URMIX, a fork of NewPipe (org.schabi.newpipe).
 */
class SoftUpdateBannerHelper @JvmOverloads constructor(
    private val context: Context,
    private val bannerRoot: View,
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
) {
    private val messageView: TextView = bannerRoot.findViewById(R.id.soft_update_banner_message)
    private val updateButton: Button = bannerRoot.findViewById(R.id.soft_update_banner_update)
    private val dismissButton: Button = bannerRoot.findViewById(R.id.soft_update_banner_dismiss)

    init {
        updateButton.setOnClickListener {
            ShareUtils.openUrlInBrowser(context, RemoteConfigRepository.getEffectiveUpdateUrl(context))
        }
        dismissButton.setOnClickListener {
            prefs.edit {
                putString(context.getString(R.string.dismissed_update_version_key), requiredVersion())
            }
            hide()
        }
    }

    /**
     * Shows the banner when a soft update is pending and not dismissed.
     *
     * @return true when the banner was shown.
     */
    fun showIfNeeded(): Boolean {
        val state = RemoteConfigRepository.getUpdateState(context)
        if (state != UpdateGate.UpdateState.SOFT_UPDATE) {
            return false
        }
        val required = requiredVersion() ?: return false
        if (prefs.getString(context.getString(R.string.dismissed_update_version_key), null) == required) {
            return false
        }
        val announcement = RemoteConfigRepository.getCachedConfig(context)?.announcement
        val title = announcement?.title?.takeIf { announcement.active && it.isNotBlank() }
        messageView.text = title ?: context.getString(R.string.soft_update_banner_message)
        show()
        return true
    }

    fun show() {
        bannerRoot.animate(true, 150)
    }

    fun hide() {
        bannerRoot.animate(false, 150)
    }

    fun isVisible(): Boolean = bannerRoot.isVisible

    fun dispose() {
        updateButton.setOnClickListener(null)
        dismissButton.setOnClickListener(null)
    }

    private fun requiredVersion(): String? = RemoteConfigRepository.getCachedConfig(context)?.appVersion

    companion object {
        private const val PREFS_NAME = "urmix_soft_update"
    }
}
