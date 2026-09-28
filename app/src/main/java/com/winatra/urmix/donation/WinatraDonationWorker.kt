package com.winatra.urmix.donation

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.winatra.urmix.MainActivity
import com.winatra.urmix.R
import com.winatra.urmix.remoteconfig.RemoteConfigRepository
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/*
 * WinatraDonationWorker — daily non-blocking support prompt (blueprint v2 §6).
 *
 * - Unique periodic work, 24 h interval, KEEP policy (max 1 enqueue).
 * - DonationPromptGate enforces max 1 visible prompt per calendar day via
 *   `last_donation_prompt_date` SharedPreferences.
 * - Notification is dismissible with zero consequences (unlike force_update).
 * - Tap opens MainActivity with EXTRA_SHOW_DONATION_SHEET, which shows the
 *   dark-themed DonationBottomSheetDialogFragment (Saweria + QRIS).
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 * This file is part of URMIX, a fork of NewPipe (org.schabi.newpipe).
 */
class WinatraDonationWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {
    override fun doWork(): Result {
        RemoteConfigRepository.refresh(applicationContext)
        val donation = RemoteConfigRepository.getCachedConfig(applicationContext)?.donation
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val notificationsOn = NotificationManagerCompat.from(applicationContext)
            .areNotificationsEnabled()
        val shouldShow = DonationPromptGate.shouldShow(
            donationAvailable = donation != null &&
                (donation.saweriaUrl != null || donation.qrisUrl != null),
            notificationsEnabled = notificationsOn,
            lastPromptDateIso = prefs.getString(KEY_LAST_PROMPT_DATE, null)
        )
        if (!shouldShow || donation == null) {
            return Result.success()
        }
        showNotification(donation.title, donation.dailyMessage)
        prefs.edit().putString(KEY_LAST_PROMPT_DATE, LocalDate.now().toString()).apply()
        return Result.success()
    }

    private fun showNotification(title: String?, message: String?) {
        val context = applicationContext
        val openIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_SHOW_DONATION_SHEET, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val openPending = PendingIntent.getActivity(
            context,
            REQUEST_OPEN,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val dismissIntent = Intent(context, DonationDismissReceiver::class.java)
        val dismissPending = PendingIntent.getBroadcast(
            context,
            REQUEST_DISMISS,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(
            context,
            context.getString(R.string.donation_notification_channel_id)
        )
            .setSmallIcon(R.drawable.ic_newpipe_triangle_white)
            .setContentTitle(title ?: context.getString(R.string.donation_notification_title))
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(openPending)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .addAction(
                0,
                context.getString(R.string.donation_notification_dismiss),
                dismissPending
            )
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val WORK_NAME = "WinatraDonationWorker"
        const val PREFS_NAME = "urmix_donation"
        const val KEY_LAST_PROMPT_DATE = "last_donation_prompt_date"
        const val NOTIFICATION_ID = 0xD0A71A
        private const val REQUEST_OPEN = 0xD0A701
        private const val REQUEST_DISMISS = 0xD0A702

        @JvmStatic
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<WinatraDonationWorker>(24, TimeUnit.HOURS)
                .addTag(WORK_NAME)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        @JvmStatic
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
