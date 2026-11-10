package com.cosmos.unreddit.ui.redditsource

import android.app.Dialog
import android.content.DialogInterface
import android.content.DialogInterface.OnShowListener
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import com.cosmos.unreddit.R
import com.cosmos.unreddit.data.model.preferences.DataPreferences
import com.cosmos.unreddit.databinding.FragmentRedditSourceBinding
import com.cosmos.unreddit.util.extension.doAndDismiss
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class RedditSourceDialogFragment : DialogFragment(), OnShowListener {

    private var _binding: FragmentRedditSourceBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = FragmentRedditSourceBinding.inflate(requireActivity().layoutInflater)
        initView()

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_reddit_source_title)
            .setView(binding.root)
            .setPositiveButton(R.string.save) { _, _ -> }
            .setNeutralButton(R.string.dialog_cancel) { dialog, _ -> dialog.dismiss() }
            .setCancelable(false)
            .create()
            .apply { setOnShowListener(this@RedditSourceDialogFragment) }
    }

    private fun initView() = binding.run {
        radioGroup.check(R.id.radio_reddit)
        radioReddit.isChecked = true
        radioRedditScrap.isVisible = false
        radioTeddit.isVisible = false
        listInstances.isVisible = false
    }

    private fun save() {
        doAndDismiss {
            setFragmentResult(
                REQUEST_KEY_SOURCE,
                bundleOf(
                    KEY_SOURCE to DataPreferences.RedditSource.REDDIT,
                    KEY_INSTANCE to ""
                )
            )
        }
    }

    override fun onShow(dialog: DialogInterface?) {
        (dialog as? AlertDialog)?.getButton(DialogInterface.BUTTON_POSITIVE)
            ?.setOnClickListener { save() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "RedditSourceDialogFragment"
        const val REQUEST_KEY_SOURCE = "REQUEST_KEY_SOURCE"
        const val KEY_SOURCE = "KEY_SOURCE"
        const val KEY_INSTANCE = "KEY_INSTANCE"

        @Suppress("UNUSED_PARAMETER")
        fun show(
            fragmentManager: FragmentManager,
            source: DataPreferences.RedditSource,
            instance: String?,
            instances: List<String>
        ) {
            RedditSourceDialogFragment().show(fragmentManager, TAG)
        }
    }
}
