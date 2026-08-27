// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

package com.infineon.secora.wallet.cdcvm

import android.content.res.ColorStateList
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.infineon.secora.wallet.R
import com.infineon.secora.wallet.databinding.ViewWearableStatusChipsBinding

/**
 * Renders the shared Bluetooth / on-body / payment status pills from a [WearableStatus].
 *
 * Wrap the include's binding once (`WearableStatusChips(binding.statusChips)`) and call [render] for
 * every [WearableStatus] the monitor produces. Each pill shows its active (teal) or inactive (grey)
 * pill background, text colour and dot tint from the state:
 * - Bluetooth: fixed label "Bluetooth conn."; active while connected.
 * - Body: "On body" (active) / "Off body" (inactive; unknown shows Off body).
 * - Payment: "Payment unlocked" (active) / "Payment locked" (inactive).
 */
class WearableStatusChips(private val binding: ViewWearableStatusChipsBinding) {

    /** Applies [status] to the three pills. */
    fun render(status: WearableStatus) {
        setChip(
            binding.tvChipBluetooth,
            active = status.connected,
            labelRes = R.string.wearable_chip_bluetooth_connected
        )

        val onBody = status.presence == BodyPresenceTracker.Presence.ON_BODY
        setChip(
            binding.tvChipBody,
            active = onBody,
            labelRes = if (onBody) R.string.wearable_chip_body_on else R.string.wearable_chip_body_off
        )

        setChip(
            binding.tvChipPayment,
            active = status.verified,
            labelRes = if (status.verified) {
                R.string.wearable_chip_payment_unlocked
            } else {
                R.string.wearable_chip_payment_locked
            }
        )
    }

    /** Shows or hides the whole pill row (e.g. hidden for NFC devices). */
    fun setVisible(visible: Boolean) {
        binding.root.isVisible = visible
    }

    /** The pill row root, e.g. to attach a tap-to-connect listener. */
    val root: View get() = binding.root

    private fun setChip(chip: TextView, active: Boolean, labelRes: Int) {
        chip.setText(labelRes)
        chip.setBackgroundResource(if (active) R.drawable.status_acti else R.drawable.status_inacti)
        val color = ContextCompat.getColor(
            chip.context,
            if (active) R.color.tealGreen else R.color.statusChipInactive
        )
        chip.setTextColor(color)
        chip.compoundDrawableTintList = ColorStateList.valueOf(color)
    }
}
