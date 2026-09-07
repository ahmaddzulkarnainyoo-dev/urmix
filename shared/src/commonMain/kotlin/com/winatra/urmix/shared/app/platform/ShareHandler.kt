/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.winatra.urmix.shared.app.platform

/**
 * Helper methods related to sharing of data and information
 * See individual platform classes for real implementation.
 */
interface ShareHandler {
    fun openUrlInBrowser(url: String)
}
