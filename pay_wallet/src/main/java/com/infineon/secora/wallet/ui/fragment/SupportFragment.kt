// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: SupportFragment.kt serves as the Help & Support screen of the app
 * providing users with quick access to contact options and support resources.
 **/
package com.infineon.secora.wallet.ui.fragment

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.net.toUri
import com.infineon.secora.wallet.R
import com.infineon.secora.wallet.databinding.FragmentCommonSupportBinding

/**
 * SupportFragment : It handles the support related feature implementation
 *
 * @property contactNumber
 * @property privacyPolicyURL
 * @property contactWebsite
 * @property termsAndConditionsURL
 * @property contactEmail
 */
class SupportFragment(
    val contactNumber: String,
    val privacyPolicyURL: String,
    val contactWebsite: String,
    val termsAndConditionsURL: String,
    val contactEmail: String,
) : BaseFragment() {

    private lateinit var binding: FragmentCommonSupportBinding

    /**
     * onCreateView method is used to inflate the layout for this fragment
     * It handles click listeners for call, mail, website, terms and privacy
     *
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCommonSupportBinding.inflate(inflater, container, false)

        binding.tvCall.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = "tel:$contactNumber".toUri()
            }
            startActivity(intent)
        }
        binding.tvMail.text = getString(R.string.email_to, contactEmail)
        binding.tvCall.text = getString(R.string.text_call, contactNumber)
        binding.tvMail.setOnClickListener {
            openDefaultMailApp(contactEmail)
        }

        binding.tvWebsite.setOnClickListener {
            openUrl(contactWebsite)
        }

        binding.tvTerms.setOnClickListener {
            openUrl(termsAndConditionsURL)
        }

        binding.tvPrivacy.setOnClickListener {
            openUrl(privacyPolicyURL)
        }

        binding.tvOpenApp.setOnClickListener {
            openOrInstallApp(requireContext(), resources.getString(R.string.open_app_package_name))
        }
        return binding.root

    }

    /**
     * Called immediately after the fragment's view has been created.
     *
     * @param view The view returned by [onCreateView].
     * @param savedInstanceState Saved instance state bundle, if any.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (contactNumber.isNotEmpty() || !contactNumber.equals("null", ignoreCase = true)) {
            binding.tvCall.text = contactNumber
        } else {
            binding.tvCall.text = ""
        }

        if (contactEmail.isNotEmpty() || !contactEmail.equals("null", ignoreCase = true)) {
            binding.tvMail.text = contactEmail
        } else {
            binding.tvMail.text = ""
        }

        if (contactWebsite.isNullOrBlank() || contactWebsite.equals("null", ignoreCase = true)) {
            binding.tvWebsite.text = ""
        }

        if (termsAndConditionsURL.isNullOrBlank() || termsAndConditionsURL.equals("null", ignoreCase = true)){
            binding.tvTerms.text = ""
        }

        if (privacyPolicyURL.isNullOrBlank() || privacyPolicyURL.equals("null", ignoreCase = true)){
            binding.tvPrivacy.text = ""
        }

    }

    /**
     * openDefaultMailApp() : Open default mail app
     *
     * @param email
     */
    private fun openDefaultMailApp(email: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = "mailto:$email".toUri()
        }
        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(requireContext(), getString(R.string.text_no_email_app_found), Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Opens [url] in a browser, or shows a toast if the URL is missing or cannot be handled.
     *
     * @param url Website, terms, or privacy policy URL from card metadata.
     */
    private fun openUrl(url: String) {
        if (url.isBlank() || url.equals("null", ignoreCase = true)) {
            Toast.makeText(requireContext(), getString(R.string.text_url_unavailable), Toast.LENGTH_SHORT).show()
            return
        }
        try {
            startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(requireContext(), getString(R.string.text_url_unavailable), Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * openOrInstallApp() : Open or install app
     *
     * @param context
     * @param packageName
     */
    private fun openOrInstallApp(context: Context, packageName: String) {
        val pm = context.packageManager
        try {
            // Try to launch the app
            val intent = pm.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                context.startActivity(intent)
            } else {
                throw PackageManager.NameNotFoundException()
            }
        } catch (_: PackageManager.NameNotFoundException) {
            // App not found, open Play Store
            try {
                val playStoreIntent = Intent(
                    Intent.ACTION_VIEW,
                    "market://details?id=$packageName".toUri()
                )
                playStoreIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(playStoreIntent)
            } catch (_: ActivityNotFoundException) {
                // Fallback if Play Store is not available
                val webIntent = Intent(
                    Intent.ACTION_VIEW,
                    "https://play.google.com/store/apps/details?id=$packageName".toUri()
                )
                webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(webIntent)
            }
        }
    }
}