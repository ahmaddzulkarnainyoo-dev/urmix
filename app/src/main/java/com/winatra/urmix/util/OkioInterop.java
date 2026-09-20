package com.winatra.urmix.util;

import java.io.File;
import okio.Path;

/*
 * OkioInterop — small Java bridge for okio path creation (FASE 5 §12.1).
 *
 * Coil 3's DiskCache.Builder needs an okio.Path, but the JVM artifact of okio
 * compiles its common-source creation functions with hidden JVM names, which
 * makes them invisible to Kotlin sources in this project (e.g. there is no
 * resolvable `String.toPath()` / `Path(...)`. The companion statics stay
 * Java-visible, so the conversion is bridged here.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 * This file is part of URMIX, a fork of NewPipe (org.schabi.newpipe).
 */
public final class OkioInterop {
    private OkioInterop() {
        // no instance
    }

    /** @return the {@link Path} for {@code file}. */
    public static Path pathOf(final File file) {
        return Path.Companion.get(file);
    }
}
