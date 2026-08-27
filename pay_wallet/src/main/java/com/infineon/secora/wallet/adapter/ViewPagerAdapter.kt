// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: ViewPagerAdapter.kt is a FragmentStateAdapter that manages and
 * Displays multiple fragments—TransactionFragment, SettingFragment, and SupportFragment—within a ViewPager.
 **/
package com.infineon.secora.wallet.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.infineon.secora.wallet.ui.fragment.CardSettingFragment
import com.infineon.secora.wallet.ui.fragment.SupportFragment
import com.infineon.secora.wallet.ui.fragment.TransactionFragment

/**
 * A [FragmentStateAdapter] that manages and displays fragments in a ViewPager2.
 *
 * This adapter is responsible for switching between:
 * 1. [TransactionFragment] – shows transaction details
 * 2. [CardSettingFragment] – shows card settings
 * 3. [SupportFragment] – shows support/contact-related information
 *
 * @param fragment The parent [Fragment] hosting this adapter.
 * @param pnoType The type of payment network operator (e.g., MDES, VTS).
 * @param cardStatus The status of the card (e.g., active, suspended).
 * @param contactNumber Customer support contact number.
 * @param privacyPolicyURL URL of the privacy policy.
 * @param contactWebsite Support website URL.
 * @param termsAndConditionsURL URL of the terms and conditions.
 * @param contactEmail Support email address.
 * @param panSuffix The last few digits of the PAN used for identifying the card.
 */
class ViewPagerAdapter(
    fragment: Fragment,
    private val pnoType: String,
    private val cardStatus: String,
    private val contactNumber: String,
    private val privacyPolicyURL: String,
    private val contactWebsite: String,
    private val termsAndConditionsURL: String,
    private val contactEmail: String,
    private val panSuffix: String
) : FragmentStateAdapter(fragment) {

    /**
     * List of fragments to be displayed in the ViewPager.
     */
    private val fragmentList = listOf(
        TransactionFragment(pnoType),
        CardSettingFragment(pnoType, cardStatus, panSuffix),
        SupportFragment(
            contactNumber,
            privacyPolicyURL,
            contactWebsite,
            termsAndConditionsURL,
            contactEmail
        )
    )

    /**
     * Returns the total number of fragments in the adapter.
     *
     * @return Number of pages in the ViewPager.
     */
    override fun getItemCount(): Int = fragmentList.size

    /**
     * Creates the fragment for the specified [position].
     *
     * @param position The position of the fragment in the ViewPager.
     * @return The corresponding [Fragment] instance.
     */
    override fun createFragment(position: Int): Fragment = fragmentList[position]
}