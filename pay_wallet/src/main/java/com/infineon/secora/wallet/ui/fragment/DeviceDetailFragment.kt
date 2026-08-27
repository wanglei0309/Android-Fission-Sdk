// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: DeviceDetailFragment.kt displays detailed information about a selected payment card and its associated device.
 * It shows the card’s branding, nickname, and expiry details, loads card and wearable images using Glide,
 * and manages navigation between Transactions, Settings, and Support tabs using a ViewPager2 with persistent tab state tracking.
 **/
package com.infineon.secora.wallet.ui.fragment

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayoutMediator
import com.infineon.secora.wallet.R
import com.infineon.secora.wallet.adapter.ViewPagerAdapter
import com.infineon.secora.wallet.data.local.StorageRepository
import com.infineon.secora.wallet.data.local.preference.PreferenceKey
import com.infineon.secora.wallet.cdcvm.WearableStatus
import com.infineon.secora.wallet.cdcvm.WearableStatusChips
import com.infineon.secora.wallet.cdcvm.WearableStatusMonitor
import com.infineon.secora.wallet.databinding.FragmentDeviceDetailBinding
import com.infineon.secora.wallet.logger.ApplicationLogger
import com.infineon.secora.wallet.logger.ApplicationLogger.Companion.getApplicationLogger
import com.infineon.secora.wallet.ui.home.MainActivity
import com.infineon.secora.wallet.domain.wearable.ble.BluetoothStateManager
import com.infineon.secora.wallet.domain.wearable.ble.BluetoothUiStateManager
import com.infineon.secora.wallet.firebase.EventBus
import com.infineon.secora.wallet.utils.constants.BundleKey
import com.infineon.secora.wallet.utils.constants.Constants
import com.infineon.secora.wallet.utils.constants.Constants.COLOR_WHITE
import com.infineon.secora.wallet.utils.StringUtils.isEmptyString
import com.infineon.secora.wallet.utils.constants.Constants.ACTION_DEVICE_STATUS_UPDATE
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * DeviceDetailFragment is used to get the details of the payment device like
 * transaction history, settings, and support.
 */
class DeviceDetailFragment : BaseFragment() {

    private val logger: ApplicationLogger = getApplicationLogger(this::class.java.simpleName)

    private var bitmap: Bitmap? = null
    private var panSuffix: String? = null
    private var foregroundColor: String? = null
    private var backgroundColor: String? = null
    private var labelColor: String? = null

    private lateinit var activity: MainActivity
    private lateinit var binding: FragmentDeviceDetailBinding
    private lateinit var pnoType: String

    /** Shared status pills (Bluetooth / on-body / payment). */
    private lateinit var statusChips: WearableStatusChips

    /** Shared poller driving the status pills. */
    private val statusMonitor = WearableStatusMonitor(onUpdate = ::onWearableStatus)

    /**
     * Job for collecting events from EventBus.
     */
    private var eventCollectorJob: Job? = null

    /**
     * Inflates the layout and initializes the view components.
     *
     * @param inflater The layout inflater used to inflate views in this fragment.
     * @param container The parent view that the fragment's UI should attach to.
     * @param savedInstanceState The saved instance state, if available.
     * @return The root view of the inflated layout.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDeviceDetailBinding.inflate(inflater, container, false)
        activity = requireActivity() as MainActivity
        binding.deviceDetails.textDeviceName.text = StorageRepository.readString(PreferenceKey.DEVICE_NAME)

        getWearableImage(
            StorageRepository.readString(PreferenceKey.DEVICE_IMAGE)
        )
        pnoType = arguments?.getString(BundleKey.PNO_TYPE).toString()
        val cardStatus = arguments?.getString(BundleKey.CARD_STATUS).toString()
        panSuffix = arguments?.getString(BundleKey.PAN_SUFFIX).toString()
        val cardNickName = arguments?.getString(BundleKey.CARD_NICK_NAME).toString()

        val defaultTabIndex = arguments?.getInt(BundleKey.DEFAULT_TAB_INDEX, -1) ?: -1
        val contactNumber = arguments?.getString(BundleKey.CONTACT_NUMBER).toString()
        val privacyPolicyURL = arguments?.getString(BundleKey.PRIVACY_POLICY_URL).toString()
        val contactWebsite = arguments?.getString(BundleKey.CONTACT_WEBSITE).toString()
        val termsAndConditionsURL = arguments?.getString(BundleKey.TERMS_AND_CONDITIONS_URL).toString()
        val contactEmail = arguments?.getString(BundleKey.CONTACT_EMAIL).toString()
        val cardExpiryDate = arguments?.getString(BundleKey.CARD_EXP_DATE).toString()
        var formatedCardExpiry = cardExpiryDate

        if (!cardExpiryDate.contains(Constants.SLASH)) {
            formatedCardExpiry = cardExpiryDate.take(2) + Constants.SLASH + cardExpiryDate.substring(2, 4)
        }
        binding.cardBranding.tvCardExpiry.text = formatedCardExpiry

        val assetId = arguments?.getString(BundleKey.ASSET_ID)
        binding.cardBranding.tvCardPan.text = getString(R.string.masked_card, panSuffix)
        binding.tvNickName.text = cardNickName
        loadLocalCardImage(assetId.toString())

        val adapter = ViewPagerAdapter(
            this,
            pnoType,
            cardStatus,
            contactNumber,
            privacyPolicyURL,
            contactWebsite,
            termsAndConditionsURL,
            contactEmail,
            panSuffix!!
        )

        binding.viewPager.adapter = adapter
        binding.viewPager.offscreenPageLimit = 2

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.text_transactions)
                1 -> getString(R.string.text_settings)
                2 -> getString(R.string.text_support)
                else -> getString(R.string.text_tab, position + 1)
            }
        }.attach()
        if (defaultTabIndex != -1) {
            // If navigating from outside (like list), open passed tab
            binding.viewPager.setCurrentItem(defaultTabIndex, false)
            TabState.historySelectedTabIndex = defaultTabIndex
        } else {
            // Restore tab
            binding.viewPager.setCurrentItem(TabState.historySelectedTabIndex, false)
        }

        // Track tab selection
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                TabState.historySelectedTabIndex = position
            }
        })
        setupBackPressedCallback()
        setupListenersAndReceivers()
        return binding.root
    }

    /**
     * Sets up event listeners and starts collecting events from EventBus
     * for card-related actions (like card refresh).
     */
    private fun setupListenersAndReceivers() {
        eventCollectorJob = viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                EventBus.events.collect { event ->
                    when (event.action) {
                        ACTION_DEVICE_STATUS_UPDATE -> handleSuspendNotification(event)
                    }
                }
            }
        }
    }

    private fun loadLocalCardImage(assetId: String) {
        lifecycleScope.launch {
            val cardData = StorageRepository.getLocalCardImageByAssetId(requireContext(), assetId)
            foregroundColor = cardData.foreGroundColor
            backgroundColor = cardData.backGroundColor
            labelColor = cardData.labelColor

            val colorInt = labelColor.toSafeColor()
            binding.cardBranding.tvCardExpiry.setTextColor(colorInt)
            binding.cardBranding.tvCardPan.setTextColor(colorInt)

            getCardImage(cardData.cardImage.toString())
        }
    }

    /**
     * Handles back press event for navigation from notification flow
     *
     */
    private fun setupBackPressedCallback() {
        val navigationFromNotification = arguments?.getBoolean(BundleKey.NAVIGATION_FROM_NOTIFICATION) == true
        logger.debug(":: Details Screen  navigationFromNotification flag : $navigationFromNotification")
        activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner, object : OnBackPressedCallback(navigationFromNotification) {
                override fun handleOnBackPressed() {
                    findNavController().navigate(R.id.deviceListFragment)
                }
            })
    }

    /**
     * Called when the fragment becomes visible and ready for user interaction.
     * This method updates screen state flags and persists them in shared preferences.
     */
    override fun onResume() {
        super.onResume()
        statusMonitor.requestPoll()
    }

    /** Renders the shared status pills (hidden for NFC) from each polled [status]. */
    private fun onWearableStatus(status: WearableStatus) {
        if (!::statusChips.isInitialized) return
        statusChips.setVisible(!status.isNfc)
        statusChips.render(status)
    }

    /**
     * The nickname value set on the setting fragment is get here through
     * its key and displayed.
     *
     * @param view The view returned by [onCreateView].
     * @param savedInstanceState Saved instance state bundle, if any.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        statusChips = WearableStatusChips(binding.statusChips)
        statusMonitor.attach(viewLifecycleOwner, requireContext())
        setupToolbarProfile(requireActivity() as AppCompatActivity)
        childFragmentManager.setFragmentResultListener(
            Constants.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            val nickname = bundle.getString(BundleKey.NICK_NAME)
            binding.tvNickName.text = nickname
        }
        updateBluetoothIconState()
    }

    /**
     * getWearableImage method is used to get the wearable image and displayed using Glide library
     */
    fun getWearableImage(image: String) {
        try {
            val imageBytes = Base64.decode(image, Base64.DEFAULT)
            Glide.with(requireContext())
                .load(imageBytes)
                .into(binding.deviceDetails.cardImageView)
        } catch (e: IllegalArgumentException) {
            logger.noStackTraceLog("GetWearableImage ", e)
        }
    }

    /**
     * getCardImage method is used to get the card image and displayed using Glide library
     */
    fun getCardImage(image: String) {
        if (isEmptyString(image)) {
            setDefaultCardImage()
        } else {
            try {
                val imageBytes = Base64.decode(image, Base64.DEFAULT)
                bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                Glide.with(requireContext())
                    .load(bitmap)
                    .error(setDefaultCardImage())
                    .into(binding.cardBranding.ivCard)
            } catch (e: IllegalArgumentException) {
                logger.noStackTraceLog("GetCardImage ", e)
            }
        }
    }

    /**
     * Lifecycle callback:
     * Registers for Bluetooth UI state updates when the screen becomes visible.
     */
    override fun onStart() {
        super.onStart()
        BluetoothUiStateManager.register(requireContext()) {
            if (isAdded) {
                updateBluetoothIconState()
            }
        }
    }

    /**
     * Lifecycle callback:
     * Unregisters the Bluetooth UI listener to avoid leaks.
     */
    override fun onStop() {
        super.onStop()
        BluetoothUiStateManager.unregister {
            updateBluetoothIconState()
        }
    }

    /**
     * Safely converts a nullable color string to an Android color [Int].
     *
     * Falls back to [COLOR_WHITE] when the value is missing, then to [default] when parsing fails.
     *
     * @param default Color [Int] returned when [toColorInt] fails on the resolved string.
     * @return Parsed color [Int], or [default] when the value is missing or invalid.
     */
    private fun String?.toSafeColor(default: Int = Color.WHITE): Int {
        val colorString = if (!isEmptyString(this)) this!! else COLOR_WHITE
        return runCatching { colorString.toColorInt() }.getOrDefault(default)
    }

    /**
     * Updates Bluetooth icon: green when BT is on and the selected device (header) is connected, black when BT off or that device disconnected.
     */
    private fun updateBluetoothIconState() {
        val selectedSeId = StorageRepository.readString(PreferenceKey.DEVICE_SE_ID)
        val showConnected = BluetoothStateManager.isBluetoothTurnedOn(requireContext())
            && BluetoothStateManager.isDeviceConnected(selectedSeId, requireContext())
        val iconRes = if (isNFC())
            R.drawable.icon_nfc
        else if (showConnected) {
            R.drawable.ic_bluetooth_connected
        } else {
            R.drawable.ic_bluetooth_disconnected
        }
        binding.deviceDetails.imgBluetooth.setImageResource(iconRes)
        binding.deviceDetails.imgBluetooth.clearColorFilter()
    }

    /*
     * Set default card image if card image is not available based on the pno type
     */
    private fun setDefaultCardImage() {
        when (pnoType) {
            "MDES" -> binding.cardBranding.ivCard.setImageResource(R.drawable.default_master)
            "VTS" -> binding.cardBranding.ivCard.setImageResource(R.drawable.default_visa)
        }
    }
}

/**
 * A singleton object used to maintain the state of tab selections across the app.
 *
 * This object stores the index of the currently selected tab in the "History" section.
 * By keeping this value here, the selected tab persists even when navigating
 * between fragments or recreating the UI.
 */
object TabState {

    // The Default value is 0, meaning the first tab is selected initially.
    var historySelectedTabIndex: Int = 0
}