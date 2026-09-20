package com.winatra.urmix.settings.export;

import android.content.Context;

import com.winatra.urmix.NewPipeDatabase;
public final class BackupResolveHelper {
    private BackupResolveHelper() {
    }

    public static void resolveLikedUidAfterLegacyZipImport(final Context appContext) {
        try {
            LibraryBackupManagerKt.resolveLikedUid(
                    appContext, NewPipeDatabase.getInstance(appContext));
        } catch (final Exception ignored) {
        }
    }
}
