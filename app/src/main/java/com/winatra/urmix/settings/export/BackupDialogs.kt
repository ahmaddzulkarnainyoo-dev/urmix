package com.winatra.urmix.settings.export
// URMIX library backup dialogs (blueprint v2 §5.2): import preview confirm +
// layman backup guide. DialogFragment pattern follows ImportConfirmationDialog.
import android.app.Dialog
import android.os.Bundle
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.livefront.bridge.Bridge
import com.winatra.urmix.R

class BackupImportPreviewDialog : DialogFragment() {
    interface Listener {
        fun onBackupImportConfirmed()
    }

    companion object {
        private const val ARG_LIKED = "liked"
        private const val ARG_PLAYLISTS = "playlists"
        private const val ARG_HISTORY = "history"

        @JvmStatic
        fun newInstance(preview: BackupPreview): BackupImportPreviewDialog {
            val dialog = BackupImportPreviewDialog()
            val args = Bundle()
            args.putInt(ARG_LIKED, preview.likedCount)
            args.putInt(ARG_PLAYLISTS, preview.playlistCount)
            args.putInt(ARG_HISTORY, preview.historyCount)
            dialog.arguments = args
            return dialog
        }
    }

    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Bridge.restoreInstanceState(this, savedInstanceState)
    }

    override fun onSaveInstanceState(@NonNull outState: Bundle) {
        super.onSaveInstanceState(outState)
        Bridge.saveInstanceState(this, outState)
    }

    @NonNull
    override fun onCreateDialog(@Nullable savedInstanceState: Bundle?): Dialog {
        val liked = requireArguments().getInt(ARG_LIKED, 0)
        val playlists = requireArguments().getInt(ARG_PLAYLISTS, 0)
        val history = requireArguments().getInt(ARG_HISTORY, 0)
        val message = getString(
            R.string.backup_preview_message,
            liked,
            playlists,
            history
        )
        return AlertDialog.Builder(requireContext())
            .setTitle(R.string.backup_preview_title)
            .setMessage(message)
            .setCancelable(true)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.ok) { _, _ ->
                (
                    parentFragment as? Listener
                        // Dialogs shown from the parent FragmentManager get parentFragment == null;
                        // walk up one level to the settings fragment in that case.
                        ?: parentFragmentManager.fragments.firstOrNull { it is Listener } as? Listener
                        ?: activity as? Listener
                    )?.onBackupImportConfirmed()
            }
            .create()
    }
}

class BackupGuideDialog : DialogFragment() {
    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Bridge.restoreInstanceState(this, savedInstanceState)
    }

    override fun onSaveInstanceState(@NonNull outState: Bundle) {
        super.onSaveInstanceState(outState)
        Bridge.saveInstanceState(this, outState)
    }

    @NonNull
    override fun onCreateDialog(@Nullable savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext())
            .setTitle(R.string.backup_guide_title)
            .setMessage(R.string.backup_guide_message)
            .setCancelable(true)
            .setPositiveButton(R.string.ok, null)
            .create()
    }

    companion object {
        @JvmStatic
        fun newInstance(): BackupGuideDialog {
            return BackupGuideDialog()
        }
    }
}
