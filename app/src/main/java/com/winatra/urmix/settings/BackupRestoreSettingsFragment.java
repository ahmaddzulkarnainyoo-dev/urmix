package com.winatra.urmix.settings;

import static org.schabi.newpipe.extractor.utils.Utils.isBlank;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

import com.grack.nanojson.JsonParserException;

import com.winatra.urmix.NewPipeDatabase;
import com.winatra.urmix.R;
import com.winatra.urmix.error.ErrorInfo;
import com.winatra.urmix.error.ErrorUtil;
import com.winatra.urmix.error.UserAction;
import com.winatra.urmix.local.subscription.SubscriptionsImportExportHelper;
import com.winatra.urmix.settings.export.BackupFileLocator;
import com.winatra.urmix.settings.export.BackupGuideDialog;
import com.winatra.urmix.settings.export.BackupImportPreviewDialog;
import com.winatra.urmix.settings.export.BackupJsonException;
import com.winatra.urmix.settings.export.BackupJsonModelsKt;
import com.winatra.urmix.settings.export.BackupPreview;
import com.winatra.urmix.settings.export.BackupResolveHelper;
import com.winatra.urmix.settings.export.BackupSchema;
import com.winatra.urmix.settings.export.ImportExportManager;
import com.winatra.urmix.settings.export.LibraryBackupExporter;
import com.winatra.urmix.settings.export.LibraryBackupImporter;
import com.winatra.urmix.settings.export.UrmixBackup;
import com.winatra.urmix.streams.io.NoFileManagerSafeGuard;
import com.winatra.urmix.streams.io.SharpOutputStream;
import com.winatra.urmix.streams.io.StoredFileHelper;
import com.winatra.urmix.util.NavigationHelper;
import com.winatra.urmix.util.ZipHelper;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BackupRestoreSettingsFragment extends BasePreferenceFragment
        implements BackupImportPreviewDialog.Listener {

    private static final String ANY_MIME_TYPE = "*/*";

    private final SimpleDateFormat exportDateFormat =
            new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
    private ImportExportManager manager;
    private String importExportDataPathKey;
    private final ActivityResultLauncher<Intent> requestImportPathLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    this::requestImportPathResult);
    private final ActivityResultLauncher<Intent> requestExportPathLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    this::requestJsonExportPathResult);
    private UrmixBackup pendingJsonBackup;
    private final ExecutorService backupWorkExecutor = Executors.newSingleThreadExecutor();
    private SubscriptionsImportExportHelper importExportHelper;


    @Override
    public void onAttach(@NonNull final Context context) {
        super.onAttach(context);
        importExportHelper = new SubscriptionsImportExportHelper(this);
    }

    @Override
    public void onCreatePreferences(@Nullable final Bundle savedInstanceState,
                                    @Nullable final String rootKey) {
        manager = new ImportExportManager(new BackupFileLocator(requireContext()));

        importExportDataPathKey = getString(R.string.import_export_data_path);

        addPreferencesFromResourceRegistry();

        final Preference guidePreference = requirePreference(R.string.backup_guide);
        guidePreference.setOnPreferenceClickListener((Preference p) -> {
            BackupGuideDialog.newInstance()
                    .show(getParentFragmentManager(), null);
            return true;
        });

        final Preference importDataPreference = requirePreference(R.string.import_data);
        importDataPreference.setOnPreferenceClickListener((Preference p) -> {
            NoFileManagerSafeGuard.launchSafe(
                    requestImportPathLauncher,
                    StoredFileHelper.getPicker(requireContext(),
                            ANY_MIME_TYPE, getImportExportDataUri()),
                    TAG,
                    getContext()
            );

            return true;
        });

        final Preference exportDataPreference = requirePreference(R.string.export_data);
        exportDataPreference.setOnPreferenceClickListener((final Preference p) -> {
            NoFileManagerSafeGuard.launchSafe(
                    requestExportPathLauncher,
                    StoredFileHelper.getNewPicker(requireContext(),
                            BackupSchema.FILE_NAME_PREFIX
                                    + exportDateFormat.format(new Date()) + ".json",
                            BackupSchema.JSON_MIME_TYPE, getImportExportDataUri()),
                    TAG,
                    getContext()
            );

            return true;
        });

        final Preference resetSettings = requirePreference(R.string.reset_settings);
        // Resets all settings by deleting shared preference and restarting the app
        // A dialogue will pop up to confirm if user intends to reset all settings
        resetSettings.setOnPreferenceClickListener(preference -> {
            // Show Alert Dialogue
            final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setMessage(R.string.reset_all_settings);
            builder.setCancelable(true);
            builder.setPositiveButton(R.string.ok, (dialogInterface, i) -> {
                // Deletes all shared preferences xml files.
                final SharedPreferences sharedPreferences =
                        PreferenceManager.getDefaultSharedPreferences(requireContext());
                sharedPreferences.edit().clear().apply();
                // Restarts the app
                if (getActivity() == null) {
                    return;
                }
                NavigationHelper.restartApp(getActivity());
            });
            builder.setNegativeButton(R.string.cancel, (dialogInterface, i) -> {
            });
            final AlertDialog alertDialog = builder.create();
            alertDialog.show();
            return true;
        });

        final Preference exportSubsPreference =
                requirePreference(R.string.export_subscriptions_key);
        exportSubsPreference.setOnPreferenceClickListener(reference -> {
            importExportHelper.onExportSelected();
            return true;
        });

        final Preference importSubsPreference =
                requirePreference(R.string.import_subscriptions_key);
        importSubsPreference.setOnPreferenceClickListener(preference -> {
            importExportHelper.onImportPreviousSelected();
            return true;
        });

    }

    private void requestJsonExportPathResult(final ActivityResult result) {
        if (result.getResultCode() != Activity.RESULT_OK
                || result.getData() == null
                || result.getData().getData() == null) {
            return;
        }
        final StoredFileHelper exportDataFile;
        try {
            exportDataFile = StoredFileHelper.deserialize(
                    new StoredFileHelper(requireActivity(), null,
                            result.getData().getData(), ""),
                    requireActivity());
        } catch (final Exception e) {
            showErrorSnackbar(e, "Exporting backup");
            return;
        }
        if (exportDataFile.isInvalid()) {
            return;
        }
        final Context appContext = requireContext().getApplicationContext();
        backupWorkExecutor.execute(() -> {
            try {
                final UrmixBackup backup = LibraryBackupExporter.INSTANCE.build(
                        appContext, NewPipeDatabase.getInstance(appContext));
                final String json = BackupJsonModelsKt.writeBackupJson(backup);
                final byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
                try (OutputStream out = new BufferedOutputStream(
                        new SharpOutputStream(
                                exportDataFile.openAndTruncateStream()))) {
                    out.write(bytes);
                    out.flush();
                }
                saveLastImportExportDataUri(exportDataFile.getUri());
                runOnUiThreadSafe(() -> {
                    if (!isAdded()) {
                        return;
                    }
                    Toast.makeText(requireContext(),
                            R.string.backup_export_complete,
                            Toast.LENGTH_LONG).show();
                });
            } catch (final Exception e) {
                runOnUiThreadSafe(() -> showErrorSnackbar(e, "Exporting backup"));
            }
        });
    }

    private void requestImportPathResult(final ActivityResult result) {
        if (result.getResultCode() != Activity.RESULT_OK
                || result.getData() == null
                || result.getData().getData() == null) {
            return;
        }
        final StoredFileHelper file;
        try {
            file = StoredFileHelper.deserialize(
                    new StoredFileHelper(requireActivity(), null,
                            result.getData().getData(), ""),
                    requireActivity());
        } catch (final Exception e) {
            showErrorSnackbar(e, "Importing backup");
            return;
        }
        if (file.isInvalid()) {
            return;
        }
        final Uri importDataUri = file.getUri();
        final String fileName = file.getName() == null ? "" : file.getName().toLowerCase();
        if (fileName.endsWith(".json")) {
            importJsonBackup(file, importDataUri);
            return;
        }
        backupWorkExecutor.execute(() -> {
            final boolean looksLikeZip;
            try {
                looksLikeZip = ZipHelper.isValidZipFile(file);
            } catch (final Exception e) {
                runOnUiThreadSafe(() -> showImportFailed(e));
                return;
            }
            if (looksLikeZip) {
                runOnUiThreadSafe(() -> confirmLegacyZipImport(file, importDataUri));
            } else {
                importJsonBackup(file, importDataUri);
            }
        });
    }

    private void importJsonBackup(final StoredFileHelper file, final Uri importDataUri) {
        backupWorkExecutor.execute(() -> {
            final UrmixBackup jsonBackup;
            try {
                jsonBackup = LibraryBackupImporter.INSTANCE.parse(file);
            } catch (final BackupJsonException e) {
                runOnUiThreadSafe(() -> showImportFailed(e));
                return;
            } catch (final Exception e) {
                runOnUiThreadSafe(() -> showImportFailed(new BackupJsonException(
                        getString(R.string.backup_import_invalid_file), e)));
                return;
            }
            pendingJsonBackup = jsonBackup;
            saveLastImportExportDataUri(importDataUri);
            final BackupPreview preview = LibraryBackupImporter.INSTANCE.preview(jsonBackup);
            runOnUiThreadSafe(() -> {
                if (!isAdded()) {
                    return;
                }
                BackupImportPreviewDialog.newInstance(preview)
                        .show(getParentFragmentManager(), null);
            });
        });
    }

    @Override
    public void onBackupImportConfirmed() {
        final UrmixBackup backup = pendingJsonBackup;
        if (backup == null) {
            return;
        }
        pendingJsonBackup = null;
        final Context appContext = requireContext().getApplicationContext();
        backupWorkExecutor.execute(() -> {
            try {
                LibraryBackupImporter.INSTANCE.apply(appContext, backup);
            } catch (final Exception e) {
                runOnUiThreadSafe(() -> showImportFailed(e));
                return;
            }
            runOnUiThreadSafe(() -> {
                if (!isAdded()) {
                    return;
                }
                Toast.makeText(requireContext(), R.string.backup_import_success,
                        Toast.LENGTH_SHORT).show();
                NavigationHelper.restartApp(requireActivity());
            });
        });
    }

    private void confirmLegacyZipImport(final StoredFileHelper file, final Uri importDataUri) {
        if (!isAdded()) {
            return;
        }
        new AlertDialog.Builder(requireContext())
                .setMessage(R.string.backup_import_legacy_zip_warning)
                .setCancelable(true)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, (d, id) -> handleLegacyZipImport(file))
                .show();
    }

    private void handleLegacyZipImport(final StoredFileHelper file) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.import_data_title)
                .setMessage(R.string.override_current_data)
                .setPositiveButton(R.string.ok, (d, id) -> {
                    importLegacyZipDatabase(file, file.getUri());
                })
                .setNegativeButton(R.string.cancel, (d, id) -> d.cancel())
                .setCancelable(true)
                .show();
    }

    private void showImportFailed(final Throwable e) {
        if (!isAdded()) {
            return;
        }
        final String detail = e.getMessage() == null ? "" : e.getMessage();
        Toast.makeText(requireContext(),
                getString(R.string.backup_import_failed, detail),
                Toast.LENGTH_LONG).show();
        showErrorSnackbar(e, "Importing backup");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        backupWorkExecutor.shutdownNow();
    }

    private void runOnUiThreadSafe(final Runnable action) {
        if (!isAdded()) {
            return;
        }
        requireActivity().runOnUiThread(action);
    }


    private void importLegacyZipDatabase(final StoredFileHelper file, final Uri importDataUri) {
        // check if file is supported
        if (!ZipHelper.isValidZipFile(file)) {
            Toast.makeText(requireContext(), R.string.no_valid_zip_file, Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        try {
            manager.ensureDbDirectoryExists();

            // replace the current database
            if (!manager.extractDb(file)) {
                Toast.makeText(requireContext(), R.string.could_not_import_all_files,
                                Toast.LENGTH_LONG)
                        .show();
            }

            // if settings file exist, ask if it should be imported.
            final boolean hasJsonPrefs = manager.exportHasJsonPrefs(file);
            if (hasJsonPrefs || manager.exportHasSerializedPrefs(file)) {
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle(R.string.import_settings)
                        .setMessage(hasJsonPrefs ? null : requireContext()
                                .getString(R.string.import_settings_vulnerable_format))
                        .setOnDismissListener(dialog -> finishLegacyZipImport(importDataUri))
                        .setNegativeButton(R.string.cancel, (dialog, which) -> {
                            dialog.dismiss();
                            finishLegacyZipImport(importDataUri);
                        })
                        .setPositiveButton(R.string.ok, (dialog, which) -> {
                            dialog.dismiss();
                            final Context context = requireContext();
                            final SharedPreferences prefs = PreferenceManager
                                    .getDefaultSharedPreferences(context);
                            try {
                                if (hasJsonPrefs) {
                                    manager.loadJsonPrefs(file, prefs);
                                } else {
                                    manager.loadSerializedPrefs(file, prefs);
                                }
                            } catch (IOException | ClassNotFoundException | JsonParserException e) {
                                createErrorNotification(e, "Importing preferences");
                                return;
                            }
                            cleanImport(context, prefs);
                            finishLegacyZipImport(importDataUri);
                        })
                        .show();
            } else {
                finishLegacyZipImport(importDataUri);
            }
        } catch (final Exception e) {
            showErrorSnackbar(e, "Importing database and settings");
        }
    }

    /**
     * Remove settings that are not supposed to be imported on different devices
     * and reset them to default values.
     * @param context the context used for the import
     * @param prefs the preferences used while running the import
     */
    private void cleanImport(@NonNull final Context context,
                             @NonNull final SharedPreferences prefs) {
        // Check if media tunnelling needs to be disabled automatically,
        // if it was disabled automatically in the imported preferences.
        final String tunnelingKey = context.getString(R.string.disable_media_tunneling_key);
        final String automaticTunnelingKey =
                context.getString(R.string.disabled_media_tunneling_automatically_key);
        // R.string.disable_media_tunneling_key should always be true
        // if R.string.disabled_media_tunneling_automatically_key equals 1,
        // but we double check here just to be sure and to avoid regressions
        // caused by possible later modification of the media tunneling functionality.
        // R.string.disabled_media_tunneling_automatically_key == 0:
        //     automatic value overridden by user in settings
        // R.string.disabled_media_tunneling_automatically_key == -1: not set
        final boolean wasMediaTunnelingDisabledAutomatically =
                prefs.getInt(automaticTunnelingKey, -1) == 1
                        && prefs.getBoolean(tunnelingKey, false);
        if (wasMediaTunnelingDisabledAutomatically) {
            prefs.edit()
                    .putInt(automaticTunnelingKey, -1)
                    .putBoolean(tunnelingKey, false)
                    .apply();
            NewPipeSettings.setMediaTunneling(context);
        }
    }

    /**
     * Save import path and restart app.
     *
     * @param importDataUri The import path to save
     */
    private void finishLegacyZipImport(final Uri importDataUri) {
        // save import path only on success
        saveLastImportExportDataUri(importDataUri);
        final Context appContext = requireContext().getApplicationContext();
        // The ZIP replaced newpipe.db on disk, so the open Room instance still points at the
        // OLD database. Close + re-resolve the Liked Songs UID against the NEW file on a
        // worker thread (Room forbids main-thread queries), then restart to properly load db.
        backupWorkExecutor.execute(() -> {
            try {
                NewPipeDatabase.close();
            } catch (final Exception e) {
                runOnUiThreadSafe(() -> showErrorSnackbar(e, "Closing database after import"));
            }
            BackupResolveHelper.resolveLikedUidAfterLegacyZipImport(appContext);
            runOnUiThreadSafe(() -> {
                if (!isAdded()) {
                    return;
                }
                // restart app to properly load db
                NavigationHelper.restartApp(requireActivity());
            });
        });
    }

    private Uri getImportExportDataUri() {
        final String path = defaultPreferences.getString(importExportDataPathKey, null);
        return isBlank(path) ? null : Uri.parse(path);
    }

    private void saveLastImportExportDataUri(final Uri importExportDataUri) {
        final SharedPreferences.Editor editor = defaultPreferences.edit()
                .putString(importExportDataPathKey, importExportDataUri.toString());
        editor.apply();
    }

    private void showErrorSnackbar(final Throwable e, final String request) {
        ErrorUtil.showSnackbar(this, new ErrorInfo(e, UserAction.DATABASE_IMPORT_EXPORT, request));
    }

    private void createErrorNotification(final Throwable e, final String request) {
        ErrorUtil.createNotification(
                requireContext(),
                new ErrorInfo(e, UserAction.DATABASE_IMPORT_EXPORT, request)
        );
    }
}
