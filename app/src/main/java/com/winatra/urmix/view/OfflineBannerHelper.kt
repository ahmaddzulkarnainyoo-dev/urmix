package com.winatra.urmix.view

import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.jakewharton.rxbinding4.view.clicks
import com.winatra.urmix.MainActivity
import com.winatra.urmix.R
import com.winatra.urmix.ktx.animate
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import java.util.concurrent.TimeUnit

/*
 * OfflineBannerHelper — compact, non-blocking offline notice (FASE 5 §12.2).
 *
 * Mirrors the structure of ErrorPanelHelper but for the *expected* no-network
 * case: instead of an error panel it shows a slim banner telling the user that
 * cached content is being displayed, with an optional retry action. The banner
 * auto-hides when connectivity returns (driven by NetworkStateObserver).
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 * This file is part of URMIX, a fork of NewPipe (org.schabi.newpipe).
 */

class OfflineBannerHelper(
    private val bannerRoot: View,
    onRetry: Runnable?
) {
    private val messageView: TextView = bannerRoot.findViewById(R.id.offline_banner_message)
    private val retryButton: Button = bannerRoot.findViewById(R.id.offline_banner_retry)

    private var retryDisposable: Disposable? = null

    init {
        retryButton.isVisible = onRetry != null
        if (onRetry != null) {
            retryDisposable = retryButton.clicks()
                .debounce(300, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { onRetry.run() }
        }
    }

    fun show() {
        bannerRoot.animate(true, 150)
    }

    fun hide() {
        bannerRoot.animate(false, 150)
    }

    fun isVisible(): Boolean = bannerRoot.isVisible

    fun setMessage(message: String) {
        messageView.text = message
    }

    fun dispose() {
        retryButton.setOnClickListener(null)
        retryDisposable?.dispose()
    }

    companion object {
        val TAG: String = OfflineBannerHelper::class.simpleName!!
        val DEBUG: Boolean = MainActivity.DEBUG
    }
}
