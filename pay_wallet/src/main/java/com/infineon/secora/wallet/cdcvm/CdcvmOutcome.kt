package com.infineon.secora.wallet.cdcvm

import com.infineon.secora.wearable.cdcvm.CvmState

/**
 * Outcome of a CDCVM operation.
 *
 * The failure cases are modelled explicitly rather than left as a generic error, because the
 * passcode screens react differently to each one: a wrong passcode offers another attempt and shows
 * the remaining count, a blocked CVM must send the user to recovery instead, and a credential
 * mismatch is a build configuration problem no amount of retrying fixes.
 */
sealed interface CdcvmOutcome {

    /**
     * Operation completed. [state] is present only for a state read.
     */
    data class Success(val state: CvmState? = null) : CdcvmOutcome

    /**
     * Passcode did not match. [remainingRetries] is the number of attempts left before the CVM
     * blocks, taken from the low nibble of the `63Cx` status word.
     */
    data class WrongPasscode(val remainingRetries: Int) : CdcvmOutcome

    /**
     * The CVM is blocked and rejects further verification attempts.
     */
    data object Blocked : CdcvmOutcome

    /**
     * No passcode has been provisioned on this device yet, so UC-01 Setup has to run first.
     */
    data object NotProvisioned : CdcvmOutcome

    /**
     * The installed OEM credentials do not match the CA that personalised this secure element, so
     * PERFORM SECURITY OPERATION was rejected. Install the issued credentials through
     * `CdcvmCredentialProvider.install(...)`.
     */
    data object CredentialMismatch : CdcvmOutcome

    /**
     * The passcode was rejected by policy before any command was sent. [reason] names the rule.
     */
    data class PolicyRejected(val reason: String) : CdcvmOutcome

    /**
     * No wearable is connected, or no SE ID is known for it.
     */
    data class NotConnected(val reason: String) : CdcvmOutcome

    /**
     * Anything else, including transport timeouts and secure channel failures. [statusWord] is the
     * status word the applet returned, or `null` when the failure was on the host side.
     */
    data class Failed(
        val message: String,
        val statusWord: Int? = null,
        val cause: Throwable? = null
    ) : CdcvmOutcome

    /**
     * `true` when the operation completed.
     */
    val isSuccess: Boolean get() = this is Success

    /**
     * Short human readable form, suitable for a log line or a toast in the bring-up harness.
     */
    fun describe(): String = when (this) {
        is Success -> state?.toString() ?: "ok"
        is WrongPasscode -> "wrong passcode, $remainingRetries attempt(s) remaining"
        Blocked -> "CVM is blocked"
        NotProvisioned -> "no passcode provisioned - run setup first"
        CredentialMismatch -> "OEM credentials do not match this secure element"
        is PolicyRejected -> "passcode rejected by policy: $reason"
        is NotConnected -> "not connected: $reason"
        is Failed ->
            if (statusWord != null) "%s (SW=%04X)".format(message, statusWord) else message
    }
}
