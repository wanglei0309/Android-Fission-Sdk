// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: ProfileFragment.kt manages the user’s profile information — including name, email, and profile picture.
 * It allows users to edit their username, capture or select a profile photo, and preview it in full view.
 **/
package com.infineon.secora.wallet.ui.fragment

import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.infineon.secora.wallet.R
import com.infineon.secora.wallet.data.local.StorageRepository
import com.infineon.secora.wallet.data.local.preference.PreferenceKey
import com.infineon.secora.wallet.databinding.DialogProfileImagePreviewBinding
import com.infineon.secora.wallet.databinding.DialogCommonMessageBinding
import com.infineon.secora.wallet.databinding.FragmentProfileUserBinding
import com.infineon.secora.wallet.domain.walletsdk.WalletRepository
import com.infineon.secora.wallet.logger.ApplicationLogger
import com.infineon.secora.wallet.logger.ApplicationLogger.Companion.getApplicationLogger
import com.infineon.secora.wallet.ui.home.MainActivity
import com.infineon.secora.wallet.utils.StringUtils.isEmptyString
import com.infineon.secora.wallet.utils.helper.showSecure
import com.infineon.secora.wallet.utils.Utils.isNetworkAvailable
import com.infineon.secora.wallet.utils.constants.Constants.SUCCESS
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

/**
 * ProfileFragment: This fragment is used to show the profile of the user.
 *
 */
class UserProfileFragment : BaseFragment() {

    private val logger: ApplicationLogger = getApplicationLogger(this::class.java.simpleName)

    private lateinit var binding: FragmentProfileUserBinding
    private lateinit var imageUri: Uri
    private lateinit var activity: MainActivity

    private var email: String = ""
    private var profileImage: String = ""
    private var username: String = ""
    private val isEditEnabled = false

    companion object {
        private const val TAG = "watcher_added"
    }

    /**
     * Launcher to handle capturing a photo using the device's camera.
     *
     * This uses [ActivityResultContracts.TakePicture] to capture an image
     * and store it in the location specified by `imageUri`.
     */
    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                try {
                    // Use the same imageUri that was passed to the camera intent
                    StorageRepository.saveString(PreferenceKey.PROFILE_IMAGE, imageUri.toString())

                    Glide.with(requireActivity())
                        .load(imageUri)
                        .circleCrop()
                        .into(binding.profileIcon)

                } catch (e: Exception) {
                    logger.noStackTraceLog("CameraLauncher ", e)
                }
            }
        }

    /**
     * Launcher to handle selecting an image from the system Photo Picker.
     *
     * Photo Picker does not require [android.Manifest.permission.READ_MEDIA_IMAGES].
     * The selected image is copied into the app’s cache directory and stored via FileProvider.
     */
    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let {
                val inputStream = requireContext().contentResolver.openInputStream(it)
                val uniqueFileName = "profile_image_${System.currentTimeMillis()}.jpg"
                val file = File(requireContext().cacheDir, uniqueFileName)
                val outputStream = FileOutputStream(file)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()

                val savedUri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.provider",
                    file
                )

                StorageRepository.saveString(PreferenceKey.PROFILE_IMAGE, savedUri.toString())
                Glide.with(requireContext())
                    .load(savedUri)
                    .circleCrop()
                    .into(binding.profileIcon)
            }
        }

    /**
     * Launcher to request camera permission before capturing a profile photo.
     */
    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                launchCamera()
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.permission_denied),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    /**
     * onCreateView method is used to inflate the layout for this fragment
     *
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentProfileUserBinding.inflate(inflater, container, false)
        activity = requireActivity() as MainActivity
        activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)

        email = StorageRepository.readString(PreferenceKey.EMAIL_ID)
        profileImage = StorageRepository.readString(PreferenceKey.PROFILE_IMAGE)
        username = StorageRepository.readString(PreferenceKey.USER_NAME)
        loadSavedImage()

        if (!isEmptyString(username)) {
            binding.username.setText(username)
            binding.username.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.light_grey
                )
            )
        } else {
            binding.username.hint = getString(R.string.enter_name)
        }

        binding.email.text = StorageRepository.readString(PreferenceKey.EMAIL_ID)
        binding.tvEdit.setOnClickListener {
            checkPermissionsAndOpenOptions()
        }
        binding.ivEditUser.visibility = View.GONE
        binding.tvEditUser.visibility = View.GONE
        binding.profileIcon.setOnClickListener {
            showImagePreview()
        }

        binding.ivEditUser.setOnClickListener {
            if (isEditEnabled) {
                showKeyboard(binding.username)
                editUserName()
            }
        }

        binding.username.setOnClickListener {
            if (isEditEnabled) {
                showKeyboard(binding.username)
                editUserName()
            }
        }
        binding.tvEditUser.setOnClickListener {
            val username = binding.username.text.toString()
            if (!isEmptyString(username)) {
                StorageRepository.saveString(PreferenceKey.USER_NAME, username)
                binding.username.apply {
                    isFocusable = false
                    isFocusableInTouchMode = false
                    isCursorVisible = false
                }
                binding.username.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.light_grey
                    )
                )
                binding.ivEditUser.visibility = View.VISIBLE
                binding.tvEditUser.visibility = View.GONE
                hideKeyboard(binding.clProfile)
                loadSavedImage()
            }
        }
        handleLogoutButtonEvent()
        dismissKeyboardOnTap(requireActivity(), binding.root)
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    handleBackPressed()
                }
            })
        return binding.root
    }

    /**
     * This method handles the logout button click event.
     *
     */
    fun handleLogoutButtonEvent() {
        binding.btnLogout.setOnClickListener {
            logoutDialog()
        }
    }


    /**
     * This method handles the logout flow,
     * Triggers the server logout api.
     * Navigates to login screen on success response or displays the error.
     *
     */
    private fun logoutDialog() {
        val dialogBindingView = DialogCommonMessageBinding.inflate(layoutInflater)
        val alertDialog = AlertDialog.Builder(requireContext())
            .setView(dialogBindingView.root)
            .create()

        dialogBindingView.txtOK.text = getString(R.string.ok)
        dialogBindingView.txtCancel.text = getString(R.string.cancel)
        dialogBindingView.txtMessage.text = getString(R.string.logout_confirmation)

        dialogBindingView.txtCancel.setOnClickListener {
            alertDialog.dismiss()
        }
        dialogBindingView.txtOK.setOnClickListener {
            if (!isNetworkAvailable(requireContext())) {
                confirmDataDialog(getString(R.string.data_enable))
                return@setOnClickListener
            }
            alertDialog.dismiss()

            lifecycleScope.launch {
                if (!::activity.isInitialized || activity.isFinishing) return@launch

                activity.showLoading(true, getString(R.string.text_please_wait))
                val sdkResult = WalletRepository.userLogout()
                activity.showLoading(false, "")

                if (sdkResult.isSuccess && sdkResult.statusMessage.equals(SUCCESS, ignoreCase = true)) {
                    navigateToLoginScreen(getString(R.string.logout_success))
                    return@launch
                }

                handleSessionOrError(sdkResult.errorMessage) {
                    val errorMessage = if (!sdkResult.errorMessage.isNullOrEmpty()) sdkResult.errorMessage else getString(R.string.logout_failed)
                    statusDialog(activity, errorMessage)
                }

            }
        }

        alertDialog.showSecure()
    }

    /**
     * editUserName(): This method is used to edit the username.
     *
     */
    fun editUserName() {
        binding.username.apply {
            isFocusable = true
            isFocusableInTouchMode = true
            isCursorVisible = true
            requestFocus()
            setSelection(text?.length ?: 0)
            hint = ""
        }

        if (binding.username.tag != TAG) {
            binding.username.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                    logger.debug("OnBeforeText:- ")
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (!s.isNullOrEmpty()) {
                        binding.tvEditUser.visibility = View.VISIBLE
                        binding.ivEditUser.visibility = View.GONE
                    } else {
                        binding.tvEditUser.visibility = View.GONE
                        binding.ivEditUser.visibility = View.VISIBLE
                    }
                }

                override fun afterTextChanged(s: Editable?) = Unit
            })
            binding.username.tag = TAG
        }
    }

    /**
     * This method is used to check the permissions and open the options.
     *
     */

    /**
     * Opens the image source dialog. Gallery uses the system Photo Picker (no storage permission).
     * Camera permission is requested only when the user chooses Camera.
     */
    private fun checkPermissionsAndOpenOptions() {
        showImageSourceDialog()
    }

    /**
     * This method is used to show the image source dialog.
     *
     */
    private fun showImageSourceDialog() {
        AlertDialog.Builder(
            requireContext(),
            androidx.appcompat.R.style.Theme_AppCompat_Dialog_Alert
        )
            .setTitle(getString(R.string.text_choose_image))
            .setItems(arrayOf(getString(R.string.text_camera), getString(R.string.text_gallery))) { _, which ->
                when (which) {
                    0 -> openCamera()
                    1 -> openGallery()
                }
            }
            .create()
            .showSecure()
    }

    /**
     *  This method is used to open the camera.
     *
     */
    private fun openCamera() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> launchCamera()

            else -> cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        val uniqueFileName = "profile_image_${System.currentTimeMillis()}.jpg"
        val imageFile = File(requireContext().cacheDir, uniqueFileName)
        imageUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            imageFile
        )
        cameraLauncher.launch(imageUri)
    }

    /**
     * Opens the system Photo Picker for image selection (no READ_MEDIA_IMAGES required).
     */
    private fun openGallery() {
        galleryLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    /**
     * Load saved image
     *
     */
    private fun loadSavedImage() {
        val activity = requireActivity() as MainActivity
        username = StorageRepository.readString(PreferenceKey.USER_NAME)
        profileImage = StorageRepository.readString(PreferenceKey.PROFILE_IMAGE)

        when {
            !isEmptyString(email) && isEmptyString(username)
                && isEmptyString(profileImage) -> {
                binding.profileIcon.setImageBitmap(
                    activity.createInitialsDrawable(email)
                )
            }

            !isEmptyString(username) && isEmptyString(profileImage) -> {
                binding.profileIcon.setImageBitmap(
                    activity.createInitialsDrawable(username)
                )
            }

            !isEmptyString(profileImage) -> {
                val uriString = StorageRepository.readString(PreferenceKey.PROFILE_IMAGE)

                if (uriString.isNotEmpty()) {
                    try {
                        val savedUri = uriString.toUri()
                        logger.debug("SavedUri: $savedUri")
                        Glide.with(requireActivity())
                            .load(savedUri)
                            .circleCrop()
                            .into(binding.profileIcon)
                    } catch (e: Exception) {
                        logger.noStackTraceLog("LoadSavedImage ", e)
                    }
                }
            }

            else -> {
                binding.profileIcon.setImageResource(R.drawable.ic_user_avatar)
            }
        }
    }

    /**
     * Show image preview
     */
    private fun showImagePreview() {
        val dialogViewBinding = DialogProfileImagePreviewBinding.inflate(layoutInflater)
        profileImage = StorageRepository.readString(PreferenceKey.PROFILE_IMAGE)

        when {
            !isEmptyString(email) && isEmptyString(username)
                && isEmptyString(profileImage) -> {
                dialogViewBinding.imagePreview.setImageBitmap(
                    (requireActivity() as MainActivity).createInitialsDrawable(
                        email
                    )
                )
            }

            !isEmptyString(username) && isEmptyString(profileImage) -> {
                dialogViewBinding.imagePreview.setImageBitmap(
                    (requireActivity() as MainActivity).createInitialsDrawable(
                        username
                    )
                )
            }

            !isEmptyString(profileImage) -> {
                try {
                    val savedUri = profileImage.toUri()
                    Glide.with(requireContext())
                        .load(savedUri)
                        .into(dialogViewBinding.imagePreview)
                } catch (e: Exception) {
                    logger.noStackTraceLog("showImagePreview", e)
                }
            }

            else -> {
                dialogViewBinding.imagePreview.setImageResource(R.drawable.ic_user_avatar)
            }
        }

        val dialog = AlertDialog.Builder(
            requireContext(),
            androidx.appcompat.R.style.Theme_AppCompat_Dialog_Alert
        )
            .setView(dialogViewBinding.root)
            .create()

        activity.showBlurryOverlay()

        // Dismiss dialog on image click
        dialogViewBinding.root.setOnClickListener {
            activity.hideBlurryOverlay()
            dialog.dismiss()
        }

        dialog.setOnCancelListener {
            activity.hideBlurryOverlay()
        }

        dialog.showSecure()
    }

    /**
     * Handles the back press action on the Profile screen.
     *
     * Checks whether the username has been modified compared to the present username.
     * - If the profile username is changed, a confirmation dialog is shown to let the user decide
     *   whether to save or discard the changes.
     */
    private fun handleBackPressed() {
        val editedUsername = binding.username.text.toString().trim()
        val actualUsername = StorageRepository.readString(PreferenceKey.USER_NAME)

        if (editedUsername != actualUsername) {
            showSaveUsernameDialog(editedUsername)
        } else {
            findNavController().popBackStack()
        }
    }

    /**
     * Displays a confirmation dialog asking the user whether to save the updated profile username.
     *
     * This dialog is shown when the user has modified their profile username but attempts
     * to navigate back without explicitly saving.
     *
     * @param editedUsername The profile username currently entered/edited by the user.
     */
    private fun showSaveUsernameDialog(editedUsername: String) {
        val dialogViewBinding = DialogCommonMessageBinding.inflate(layoutInflater)

        val alertDialog = AlertDialog.Builder(
            requireContext(),
            androidx.appcompat.R.style.Theme_AppCompat_Dialog_Alert
        )
            .setView(binding.root)
            .setCancelable(true)
            .create()

        alertDialog.showSecure()

        alertDialog.window?.setBackgroundDrawable(
            ContextCompat.getDrawable(requireContext(), R.drawable.bg_on_selected)
        )
        alertDialog.window?.decorView?.setPadding(0, 0, 0, 0)

        val width = (resources.displayMetrics.widthPixels * 0.80).toInt()
        alertDialog.window?.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)
        alertDialog.window?.setGravity(Gravity.CENTER)

        dialogViewBinding.txtMessage.text = getString(R.string.username_update_confirmation)
        dialogViewBinding.txtOK.text = getString(R.string.text_yes)
        dialogViewBinding.txtCancel.text = getString(R.string.text_no)

        dialogViewBinding.txtOK.setOnClickListener {
            StorageRepository.saveString(PreferenceKey.USER_NAME, editedUsername)
            resetUserNameField()
            hideKeyboard(binding.root)
            alertDialog.dismiss()
            findNavController().popBackStack()
        }
        dialogViewBinding.txtCancel.setOnClickListener {
            resetUserNameField()
            hideKeyboard(binding.root)
            alertDialog.dismiss()
            findNavController().popBackStack()
        }
    }

    /**
     * Resets the profile username input field to a non-editable state.
     *
     * This method is called after saving or canceling profile username edits.
     * and re-enables the "Edit" icon for future edits.
     */
    private fun resetUserNameField() = with(binding) {
        username.apply {
            isFocusable = false
            isFocusableInTouchMode = false
            isCursorVisible = false
            setTextColor(ContextCompat.getColor(requireContext(), R.color.light_grey))
        }
        ivEditUser.visibility = View.VISIBLE
        tvEditUser.visibility = View.GONE
    }

}