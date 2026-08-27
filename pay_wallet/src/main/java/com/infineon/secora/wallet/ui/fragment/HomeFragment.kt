// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: HomeFragment.kt displays the user’s profile and basic home screen content.
 * It loads user details name, email,and profile image
 * from shared preferences, updates the toolbar title, and dynamically sets the profile image—using initials or a stored image.
 **/
package com.infineon.secora.wallet.ui.fragment

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.infineon.secora.wallet.R
import com.infineon.secora.wallet.data.local.StorageRepository
import com.infineon.secora.wallet.data.local.preference.PreferenceKey
import com.infineon.secora.wallet.databinding.FragmentMainHomeBinding
import com.infineon.secora.wallet.logger.ApplicationLogger
import com.infineon.secora.wallet.logger.ApplicationLogger.Companion.getApplicationLogger
import com.infineon.secora.wallet.ui.home.MainActivity
import com.infineon.secora.wallet.utils.StringUtils.isEmptyString

/**
 * HomeFragment is used to show the home screen
 */
class HomeFragment : BaseFragment() {

    private val logger: ApplicationLogger = getApplicationLogger(this::class.java.simpleName)
    private lateinit var binding: FragmentMainHomeBinding

    private var userName: String = ""
    private var profileImage: String = ""
    private var email: String = ""

    /**
     * Called to create and return the view hierarchy associated with the HomeFragment.
     *
     * @param inflater The LayoutInflater used to inflate views in the fragment.
     * @param container The parent view that the fragment's UI should be attached to.
     * @param savedInstanceState Previously saved state (if any).
     * @return The root View of the fragment layout.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainHomeBinding.inflate(inflater, container, false)

        val context = requireContext()
        val activity = requireActivity() as MainActivity

        userName = StorageRepository.readString(PreferenceKey.USER_NAME)
        profileImage = StorageRepository.readString(PreferenceKey.PROFILE_IMAGE)
        email = StorageRepository.readString(PreferenceKey.EMAIL_ID)
        binding.btnReadMore.setOnClickListener {
            statusDialog(context, getString(R.string.read_more_toast))
        }
        (activity).binding.toolbar.toolbarTitle.text = getString(R.string.menu_home)

        when {
            !isEmptyString(email) && isEmptyString(userName)
                && isEmptyString(profileImage) -> {
                binding.ivProfile.setImageBitmap(
                    activity.createInitialsDrawable(email)
                )
            }

            !isEmptyString(userName) && isEmptyString(profileImage) -> {
                binding.ivProfile.setImageBitmap(
                    activity.createInitialsDrawable(userName)
                )
            }

            !isEmptyString(profileImage) -> {
                val uriString = profileImage

                if (uriString.isNotEmpty()) {
                    try {
                        val savedUri = uriString.toUri()
                        Glide.with(requireActivity())
                            .load(savedUri)
                            .circleCrop()
                            .into(binding.ivProfile)
                    } catch (e: Exception) {
                        logger.noStackTraceLog("onCreateView ", e)
                    }
                }
            }

            else -> {
                binding.ivProfile.setImageResource(R.drawable.ic_user_avatar)
            }
        }
        return binding.root
    }

    /**
     * onViewCreated(): Called when the view is created.
     *
     * @param view
     * @param savedInstanceState
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as MainActivity).binding.toolbar.toolbarTitle.text =
            getString(R.string.menu_home)
        (requireActivity() as MainActivity).binding.toolbar.profileIcon.visibility = View.GONE
        refreshProfileIcon()
    }

    /**
     * refreshProfileIcon(): Refreshes the profile icon.
     */
    fun refreshProfileIcon() {
        val activity = requireActivity() as MainActivity
        userName = StorageRepository.readString(PreferenceKey.USER_NAME)
        profileImage = StorageRepository.readString(PreferenceKey.PROFILE_IMAGE)
        email = StorageRepository.readString(PreferenceKey.EMAIL_ID)
        binding.ivProfile.setImageDrawable(null)

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.POST_NOTIFICATIONS
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                1001
            )
        }

        when {
            !isEmptyString(email) && isEmptyString(userName)
                && isEmptyString(profileImage) -> {
                binding.ivProfile.setImageBitmap(
                    activity.createInitialsDrawable(email)
                )
            }

            !isEmptyString(userName) && isEmptyString(profileImage) -> {
                binding.ivProfile.setImageBitmap(
                    activity.createInitialsDrawable(userName)
                )
            }

            !isEmptyString(profileImage) -> {
                val uriString = StorageRepository.readString(PreferenceKey.PROFILE_IMAGE)

                if (uriString.isNotEmpty()) {
                    try {
                        val savedUri = uriString.toUri()
                        Glide.with(requireActivity())
                            .load(savedUri)
                            .circleCrop()
                            .into(binding.ivProfile)
                    } catch (e: Exception) {
                        logger.noStackTraceLog("refreshProfileIcon ", e)
                    }
                }
            }

            else -> {
                binding.ivProfile.setImageResource(R.drawable.ic_user_avatar)
            }
        }
    }
}