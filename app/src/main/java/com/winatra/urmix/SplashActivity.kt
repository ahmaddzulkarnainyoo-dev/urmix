package com.winatra.urmix

/*
 * URMIX (WINATRA) — splash launcher activity.
 *
 * Shows the URMIX brand for ~1.6 s (blueprint §2), fetches the WINATRA remote
 * config in the background (§1.2) and then hands over to MainActivity, unless a
 * cached config demands a blocking forced update.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 * This file is part of URMIX, a fork of NewPipe (org.schabi.newpipe).
 */

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.appcompat.app.AppCompatActivity
import com.winatra.urmix.databinding.ActivitySplashBinding
import com.winatra.urmix.remoteconfig.RemoteConfigRepository
import com.winatra.urmix.util.external_communication.ShareUtils

class SplashActivity : AppCompatActivity() {

    /**
     * How long the splash screen is shown before the main activity is started.
     * The system splash (window background) is already on screen before this
     * activity is entered, so there is no visible gap.
     */
    private val splashDurationMs = 1600L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivitySplashBinding.inflate(getLayoutInflater())
        setContentView(binding.root)

        // Gentle fade-in of the URMIX brand block.
        binding.splashBrandBlock.alpha = 0.0f
        binding.splashBrandBlock.animate().alpha(1.0f).setDuration(600).start()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        val splashStartedAt = SystemClock.elapsedRealtime()
        val mainHandler = Handler(Looper.getMainLooper())
        Thread({
            // Refresh completes with either the new config or the existing cache.
            RemoteConfigRepository.refresh(applicationContext)
            val remainingSplashTime =
                (splashDurationMs - (SystemClock.elapsedRealtime() - splashStartedAt))
                    .coerceAtLeast(0L)
            mainHandler.postDelayed(
                { openMainActivityOrBlock() },
                remainingSplashTime
            )
        }, "urmix-remote-config").start()
    }

    private fun openMainActivityOrBlock() {
        if (RemoteConfigRepository.shouldBlockStartup(applicationContext)) {
            showForceUpdateDialog()
            return
        }
        startMainActivity()
    }

    private fun showForceUpdateDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.force_update_dialog_title)
            .setMessage(R.string.force_update_dialog_message)
            .setCancelable(false)
            .setPositiveButton(R.string.force_update_dialog_update) { _, _ ->
                ShareUtils.openUrlInBrowser(this, RemoteConfigRepository.getEffectiveUpdateUrl(this))
                finish()
            }
            .setNeutralButton(R.string.force_update_dialog_exit) { _, _ ->
                finish()
            }
            .show()
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }
}
