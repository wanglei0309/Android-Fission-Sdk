// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: ScreenCaptureProtection.kt applies or clears screen-capture protection on app
 * activity and dialog windows. It sets [WindowManager.LayoutParams.FLAG_SECURE] when
 * [BuildConfig.PREVENT_SCREEN_CAPTURE] is enabled to block screenshots and screen recording,
 * and enables tapjacking protection on the window decor view.
 **/
package com.infineon.secora.wallet.utils.helper

import android.app.Activity
import android.app.Dialog
import android.view.Window
import android.view.WindowManager
import com.infineon.secora.wallet.BuildConfig

/**
 * ScreenCaptureProtection: Helper that applies or clears [WindowManager.LayoutParams.FLAG_SECURE]
 * based on [BuildConfig.PREVENT_SCREEN_CAPTURE] and enables overlay (tapjacking) filtering.
 */
object ScreenCaptureProtection {

    /**
     * Applies or clears screen-capture protection on the host activity window.
     *
     * @param activity Host activity whose window is protected when [BuildConfig.PREVENT_SCREEN_CAPTURE] is enabled.
     */
    fun apply(activity: Activity) {
        val window = activity.window
        window.decorView.setFilterTouchesWhenObscured(true)
        if (BuildConfig.PREVENT_SCREEN_CAPTURE) {
            activity.getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        } else {
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    /**
     * Applies or clears screen-capture protection on a dialog window before it is shown.
     *
     * @param dialog Dialog whose window receives [WindowManager.LayoutParams.FLAG_SECURE] when prevention is enabled.
     */
    fun applyToDialog(dialog: Dialog) {
        applyToWindow(dialog.window)
    }

    /**
     * Sets or clears [WindowManager.LayoutParams.FLAG_SECURE] on [window] based on [BuildConfig.PREVENT_SCREEN_CAPTURE],
     * and always filters touches when the window is obscured by another window (tapjacking defense).
     *
     * @param window Target window; no-op when `null`.
     */
    fun applyToWindow(window: Window?) {
        if (window == null) return
        // Keep the explicit method call form so static analyzers can detect tapjacking protection.
        window.decorView.setFilterTouchesWhenObscured(true)
        if (BuildConfig.PREVENT_SCREEN_CAPTURE) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}

/**
 * Shows this dialog with [WindowManager.LayoutParams.FLAG_SECURE] applied when screen capture prevention is enabled.
 */
fun Dialog.showSecure() {
    ScreenCaptureProtection.applyToDialog(this)
    show()
}
