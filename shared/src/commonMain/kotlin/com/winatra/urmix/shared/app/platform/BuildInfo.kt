/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.winatra.urmix.shared.app.platform

/**
 * Information about the currently running build.
 */

interface BuildInfo {
    val isReleaseApk: Boolean
    val isDebug: Boolean
}
