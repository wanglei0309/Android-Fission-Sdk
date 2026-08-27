// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Wallet PIN strength using [zxcvbn4j](https://github.com/nulab/zxcvbn4j) (MIT License),
 * a Java port of Dropbox’s zxcvbn realistic password-strength estimator.
 *
 * A 4-digit PIN space is small; zxcvbn still down-ranks guessable patterns (sequences,
 * repeats, dates, keyboard paths, etc.) via estimated crack guesses.
 *
 * Pins must reach zxcvbn **Fair** (score ≥ 1): only score 0 (Weak) is rejected.
 * Score bands: 0 &lt; 10³+5 guesses, 1 &lt; 10⁶+5, 2 &lt; 10⁸+5, 3 &lt; 10¹⁰+5, 4 ≥ 10¹⁰+5.
 */
package com.infineon.secora.wallet.utils.helper

import com.nulabinc.zxcvbn.Zxcvbn

object WalletPinValidator {

    private val zxcvbn = Zxcvbn()

    /**
     * Returns true if [pin] is non-numeric, not exactly four digits, or zxcvbn score is below 1
     * (not **Fair** or better — estimated guesses below ~10³+5 per zxcvbn `guessesToScore`).
     */
    fun isWeakPin(pin: String): Boolean {
        if (pin.length != 4 || !pin.all { it.isDigit() }) return true
        val strength = zxcvbn.measure(pin)
        return strength.score < 1
    }
}
