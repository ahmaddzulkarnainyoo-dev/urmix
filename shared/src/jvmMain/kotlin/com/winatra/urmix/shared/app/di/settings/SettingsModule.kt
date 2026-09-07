/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.winatra.urmix.shared.app.di.settings

import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import java.util.prefs.Preferences
import org.koin.core.annotation.Singleton

/**
 * Settings for JVM devices based on Java Preferences
 */
@Singleton
fun provideSettings(): Settings = PreferencesSettings(Preferences.userRoot())
