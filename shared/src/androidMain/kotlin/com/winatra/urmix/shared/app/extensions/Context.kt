/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.winatra.urmix.shared.app.extensions

import android.content.Context
import android.content.Intent
import kotlinx.serialization.json.Json
import com.winatra.urmix.shared.Constants
import com.winatra.urmix.shared.app.ComposeActivity
import com.winatra.urmix.shared.app.navigation.Destination

/**
 * Navigates to a given compose destination
 */
fun Context.navigateTo(destination: Destination) = Intent(this, ComposeActivity::class.java).also { intent ->
    intent.putExtra(Constants.INTENT_SCREEN_KEY, Json.encodeToString(destination))
    startActivity(intent)
}
