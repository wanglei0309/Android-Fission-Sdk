// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

package com.infineon.secora.wallet.ui.fragment

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.infineon.secora.wallet.R
import com.infineon.secora.wallet.cdcvm.CdcvmApi
import com.infineon.secora.wallet.cdcvm.CdcvmOutcome
import com.infineon.secora.wallet.databinding.DialogCommonMessageBinding
import com.infineon.secora.wallet.databinding.FragmentWearablePasscodeBinding
import com.infineon.secora.wallet.logger.ApplicationLogger
import com.infineon.secora.wallet.logger.ApplicationLogger.Companion.getApplicationLogger
import com.infineon.secora.wallet.ui.home.MainActivity
import com.infineon.secora.wallet.ui.widget.AsteriskPasswordTransformationMethod
import com.infineon.secora.wallet.utils.helper.UIHelper
import com.infineon.secora.wallet.utils.helper.showSecure
import com.infineon.secora.wearable.cdcvm.PasscodePolicy
import kotlinx.coroutines.launch

/**
 * Every step of the wearable payment passcode flows: UC-01 setup, UC-02 verify and UC-07 change.
 *
 * The step is passed in [ARG_STEP]. All six steps render the same screen, so one fragment drives
 * them and [WearablePasscodeStep] supplies the copy and decides what submitting does. Steps that
 * only gather input navigate to the next step carrying what has been entered so far; steps that
 * talk to the secure element call [CdcvmApi] and translate the [CdcvmOutcome].
 *
 * Navigate to it with [argsFor]:
 * ```
 * findNavController().navigate(
 *     R.id.wearablePasscodeFragment,
 *     WearablePasscodeFragment.argsFor(WearablePasscodeStep.VERIFY)
 * )
 * ```
 */
class WearablePasscodeFragment : BaseFragment() {

    companion object {
        /** Which [WearablePasscodeStep] to render. */
        const val ARG_STEP: String = "wearablePasscodeStep"

        /** Passcode carried from the first screen of setup or change into its confirm screen. */
        const val ARG_PENDING_PASSCODE: String = "wearablePendingPasscode"

        /** Current passcode carried through the change flow so the final step can send both. */
        const val ARG_CURRENT_PASSCODE: String = "wearableCurrentPasscode"
        const val ARG_NAVIGATE_TO_SETTINGS: String = "wearableNavigationToSettings"

        /**
         * Builds the arguments for a step.
         *
         * @param step Step to render.
         * @param pendingPasscode Passcode chosen on a previous screen, for the confirm steps.
         * @param currentPasscode Existing passcode, for the change flow.
         */
        fun argsFor(
            step: WearablePasscodeStep,
            pendingPasscode: String? = null,
            currentPasscode: String? = null,
            navigateToSettings: Boolean = false,
        ): Bundle = Bundle().apply {
            putString(ARG_STEP, step.name)
            pendingPasscode?.let { putString(ARG_PENDING_PASSCODE, it) }
            currentPasscode?.let { putString(ARG_CURRENT_PASSCODE, it) }
            putBoolean(ARG_NAVIGATE_TO_SETTINGS, navigateToSettings)
        }
    }

    private val logger: ApplicationLogger = getApplicationLogger(this::class.java.simpleName)
    private lateinit var binding: FragmentWearablePasscodeBinding
    private lateinit var activity: MainActivity

    private val step: WearablePasscodeStep by lazy {
        arguments?.getString(ARG_STEP)
            ?.let { runCatching { WearablePasscodeStep.valueOf(it) }.getOrNull() }
            ?: WearablePasscodeStep.VERIFY
    }

    private val pendingPasscode: String? by lazy { arguments?.getString(ARG_PENDING_PASSCODE) }
    private val currentPasscode: String? by lazy { arguments?.getString(ARG_CURRENT_PASSCODE) }
    private val navigateToSettings: Boolean? by lazy { arguments?.getBoolean(ARG_NAVIGATE_TO_SETTINGS) }

    /** Guards against a second submit while an APDU exchange is still running. */
    private var submitting = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWearablePasscodeBinding.inflate(inflater, container, false)
        activity = requireActivity() as MainActivity

        binding.tvTitle.text = getString(step.titleRes)
        binding.tvMessage.text = getString(step.messageRes)
        binding.btnPasscode.text = getString(step.actionRes)
        activity.binding.toolbar.toolbarTitle.text = getString(R.string.wearable_passcode_toolbar)

        maskDigits()
        UIHelper.setupOtpBoxes(digitBoxes())
        UIHelper.disablePasteOnInputs(requireContext(), *digitBoxes().toTypedArray())
        binding.btnPasscode.setOnClickListener { onSubmit() }
        dismissKeyboardOnTap(requireActivity(), binding.root)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        UIHelper.showKeyboard(requireContext(), binding.etBox1)
        binding.etBox1.requestFocus()
    }

    private fun digitBoxes() =
        listOf(binding.etBox1, binding.etBox2, binding.etBox3, binding.etBox4)

    private fun maskDigits() {
        digitBoxes().forEach { it.transformationMethod = AsteriskPasswordTransformationMethod() }
    }

    private fun enteredPasscode(): String =
        digitBoxes().joinToString("") { it.text.toString().trim() }

    /**
     * Validates the input, then either advances the flow or sends the command for this step.
     */
    private fun onSubmit() {
        if (submitting) {
            return
        }
        clearError()
        val passcode = enteredPasscode()

        if (passcode.length != PASSCODE_DIGITS) {
            showError(getString(R.string.wearable_passcode_incomplete))
            return
        }

        // A passcode that will be provisioned has to satisfy the policy. Checking here keeps the
        // weak-passcode case off the secure element entirely.
        if (step.collectsNewPasscode && !PasscodePolicy.isValid(passcode)) {
            showError(getString(R.string.wearable_passcode_weak))
            clearDigits()
            return
        }

        // Confirm steps must match what was entered on the previous screen.
        if (!step.collectsNewPasscode && pendingPasscode != null && passcode != pendingPasscode) {
            showError(getString(R.string.wearable_passcode_mismatch))
            clearDigits()
            return
        }

        if (step.sendsCommand) {
            sendCommand(passcode)
        } else {
            advance(passcode)
        }
    }

    /**
     * Moves to the next input step, carrying forward what has been entered.
     */
    private fun advance(passcode: String) {
        val args = when (step) {
            WearablePasscodeStep.SETUP ->
                argsFor(WearablePasscodeStep.CONFIRM_SETUP, pendingPasscode = passcode)

            WearablePasscodeStep.CHANGE_CURRENT ->
                argsFor(WearablePasscodeStep.CHANGE_NEW, currentPasscode = passcode)

            WearablePasscodeStep.CHANGE_NEW ->
                argsFor(
                    WearablePasscodeStep.CHANGE_CONFIRM,
                    pendingPasscode = passcode,
                    currentPasscode = currentPasscode
                )

            else -> {
                logger.debug("No next step defined for $step")
                return
            }
        }
        findNavController().navigate(R.id.wearablePasscodeFragment, args)
    }

    /**
     * Runs the secure-element command for this step and reports the outcome.
     *
     * BLE is ensured first, because every one of these commands needs a connected wearable.
     */
    private fun sendCommand(passcode: String) {
        ensureBleConnectedThenRun(
            onConnected = {
                submitting = true
                setBusy(true)
                lifecycleScope.launch {
                    val outcome = when (step) {
                        WearablePasscodeStep.CONFIRM_SETUP ->
                            CdcvmApi.setupPasscode(requireContext(), passcode)

                        WearablePasscodeStep.VERIFY ->
                            CdcvmApi.verifyPasscode(requireContext(), passcode)

                        WearablePasscodeStep.CHANGE_CONFIRM ->
                            CdcvmApi.changePasscode(
                                requireContext(),
                                currentPasscode.orEmpty(),
                                passcode
                            )

                        else -> CdcvmOutcome.Failed("unsupported step $step")
                    }
                    submitting = false
                    setBusy(false)
                    handleOutcome(outcome)
                }
            },
            onCancelled = {
                submitting = false
                setBusy(false)
            }
        )
    }

    /**
     * Turns an outcome into either a success exit or an inline error the user can act on.
     */
    private fun handleOutcome(outcome: CdcvmOutcome) {
        logger.debug("CDCVM $step: ${outcome.describe()}")
        when (outcome) {
            is CdcvmOutcome.Success -> {
                showToast(getString(successMessageRes()))
                exitFlow()
            }

            is CdcvmOutcome.WrongPasscode -> {
                clearDigits()
                if (outcome.remainingRetries == 0) {

                    val dialogBinding = DialogCommonMessageBinding.inflate(layoutInflater)
                    val dialog = Dialog(requireContext())
                    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                    dialog.setContentView(dialogBinding.root)
                    dialog.setCancelable(false)
                    dialogBinding.txtCancel.visibility = View.GONE

                    dialogBinding.txtTitle.text = getString(R.string.dialog_title)
                    dialogBinding.txtMessage.text = getString(R.string.wearable_clear_device_locked)
                    dialogBinding.txtOK.setOnClickListener {
                        dialog.dismiss()
                        findNavController().navigate(R.id.cardListFragment)
                    }
                    dialog.showSecure()

                    return

                }
                showError(
                    if (outcome.remainingRetries > 0) {
                        getString(R.string.wearable_passcode_wrong, outcome.remainingRetries)
                    } else {
                        getString(R.string.wearable_passcode_wrong_generic)
                    }
                )
            }

            CdcvmOutcome.Blocked -> {
                showError(getString(R.string.wearable_passcode_blocked))
                clearDigits()
            }

            CdcvmOutcome.NotProvisioned -> {
                showError(getString(R.string.wearable_passcode_not_provisioned))
            }

            is CdcvmOutcome.NotConnected -> {
                showError(getString(R.string.wearable_passcode_not_connected))
            }

            is CdcvmOutcome.PolicyRejected -> {
                showError(getString(R.string.wearable_passcode_weak))
                clearDigits()
            }

            CdcvmOutcome.CredentialMismatch, is CdcvmOutcome.Failed -> {
                showError(getString(R.string.wearable_passcode_failed))
                clearDigits()
            }
        }
    }

    private fun successMessageRes(): Int = when (step) {
        WearablePasscodeStep.CONFIRM_SETUP -> R.string.wearable_passcode_setup_success
        WearablePasscodeStep.VERIFY -> R.string.wearable_passcode_verify_success
        else -> R.string.wearable_passcode_change_success
    }

    /**
     * Leaves the passcode flow once a step succeeded, returning to whatever launched it.
     */
    private fun exitFlow() {
        hideKeyboard(binding.root)
        if (navigateToSettings == true) {
            findNavController().navigate(R.id.wearableSettingFragment)
            return
        }
        if (!findNavController().popBackStack(R.id.cardListFragment, false)) {
            findNavController().popBackStack()
        }
    }

    private fun setBusy(busy: Boolean) {
        binding.btnPasscode.isEnabled = !busy
        activity.showLoading(busy, getString(R.string.wearable_passcode_in_progress))
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
    }

    private fun clearError() {
        binding.tvError.visibility = View.INVISIBLE
    }

    private fun clearDigits() {
        digitBoxes().forEach { it.text?.clear() }
        UIHelper.showKeyboard(requireContext(), binding.etBox1)
        binding.etBox1.requestFocus()
    }
}

/** Number of digits in a wearable payment passcode. */
private const val PASSCODE_DIGITS = 4
