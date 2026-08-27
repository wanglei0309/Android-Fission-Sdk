// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: NfcScanBottomSheet is bottom sheet ui, which used during NFC processing.
 **/
package com.infineon.secora.wallet.ui.sheet

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.infineon.secora.wallet.databinding.SheetDevicesNfcScanBinding
import com.infineon.secora.wallet.utils.helper.ScreenCaptureProtection

class NfcScanBottomSheet : BottomSheetDialogFragment() {

    private var _binding: SheetDevicesNfcScanBinding? = null
    private val binding get() = _binding!!

    var isProgrammaticDismiss = false
    var onCancelClick: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SheetDevicesNfcScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        binding.btnCancel.setOnClickListener {
            onCancelClick?.invoke()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { ScreenCaptureProtection.applyToWindow(it) }

        val bottomSheet =
            dialog?.findViewById<View>(
                R.id.design_bottom_sheet
            )

        bottomSheet?.post {
            val layoutParams =
                bottomSheet.layoutParams as ViewGroup.MarginLayoutParams

            layoutParams.marginStart = 32
            layoutParams.marginEnd = 32
            layoutParams.bottomMargin = 0

            bottomSheet.layoutParams = layoutParams
            bottomSheet.setBackgroundResource(android.R.color.transparent)
        }
    }
}