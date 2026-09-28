package com.winatra.urmix.donation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat

/**
 * Dismiss action receiver for the daily donation notification (FASE 6 §6).
 *
 * Cancels the notification with zero consequences — explicitly distinct from
 * the blocking force-update dialog.
 */
class DonationDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NotificationManagerCompat.from(context).cancel(WinatraDonationWorker.NOTIFICATION_ID)
    }
}
