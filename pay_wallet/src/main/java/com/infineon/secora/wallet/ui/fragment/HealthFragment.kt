// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: HealthFragment.kt inflates and displays the health-related UI layout using View Binding.
 **/
package com.infineon.secora.wallet.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.infineon.secora.wallet.databinding.FragmentMainHealthBinding

/**
 * This is for future implementation
 */
class HealthFragment : BaseFragment() {

    private lateinit var binding: FragmentMainHealthBinding

    /**
     * Called to have the fragment instantiate its user interface view.
     *
     * @param inflater The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The root View for the fragment's layout.
     *
     * This method inflates the fragment's layout using View Binding and returns the root view.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainHealthBinding.inflate(inflater, container, false)
        return binding.root
    }
}