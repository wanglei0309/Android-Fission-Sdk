// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

package com.infineon.secora.wallet

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.infineon.secora.wallet.data.local.StorageRepository
import com.infineon.secora.wallet.data.local.preference.PreferenceKey
import com.infineon.secora.wallet.domain.wearable.ble.BluetoothStateManager
import com.infineon.secora.wallet.logger.ApplicationLogger.Companion.getApplicationLogger
import com.infineon.secora.wallet.utils.constants.BundleKey
import com.infineon.secora.wallet.utils.helper.ConfiguredWalletIdentity
import com.infineon.secora.wearable.protocolapi.IHostSharedBleProtocol
import java.util.Locale

/**
 * 宿主 demo 在主页用 Fission SDK 完成扫描/连接后，带着 MAC 跳进支付模块时的衔接层。
 *
 * Fission 主 BLE 保持不断开；SECORA 走共享 GATT，由宿主注册 [BluetoothStateManager.registerHostProtocol]。
 */
object PayExternalLaunch {

    const val EXTRA_PRECONNECTED = "pay_external_preconnected"
    const val EXTRA_FISSION_BLE_CONNECTED = "pay_fission_ble_connected"
    const val EXTRA_HOST_SECORA_READY = "pay_host_secora_ready"

    private val logger = getApplicationLogger("PayExternalLaunch")

    fun copyLaunchExtras(from: Intent?, to: Intent) {
        if (from == null) {
            return
        }
        val mac = from.getStringExtra(BundleKey.DEVICE_BLE_ADDRESS)?.trim().orEmpty()
        if (mac.isEmpty()) {
            return
        }
        to.putExtra(BundleKey.DEVICE_BLE_ADDRESS, mac)
        to.putExtra(EXTRA_PRECONNECTED, from.getBooleanExtra(EXTRA_PRECONNECTED, true))
        to.putExtra(EXTRA_FISSION_BLE_CONNECTED, from.getBooleanExtra(EXTRA_FISSION_BLE_CONNECTED, false))
        to.putExtra(EXTRA_HOST_SECORA_READY, from.getBooleanExtra(EXTRA_HOST_SECORA_READY, false))
        from.getStringExtra(BundleKey.DEVICE_NAME)?.takeIf { it.isNotBlank() }?.let {
            to.putExtra(BundleKey.DEVICE_NAME, it)
        }
    }

    fun markHostLaunchFromIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_PRECONNECTED, false) == true) {
            StorageRepository.saveBoolean(PreferenceKey.HOST_LAUNCH_ACTIVE, true)
        }
    }

    fun isHostLaunch(): Boolean =
        StorageRepository.readBoolean(PreferenceKey.HOST_LAUNCH_ACTIVE)

    fun addressesMatch(a: String?, b: String?): Boolean {
        if (a.isNullOrBlank() || b.isNullOrBlank()) return false
        fun canonical(value: String) =
            value.trim().uppercase(Locale.US).replace(":", "").replace("-", "")
        return canonical(a) == canonical(b)
    }

    /** MAC of the wearable passed in from the Fission host demo. */
    fun isHostSelectedDeviceAddress(address: String?): Boolean {
        if (!isHostLaunch() || address.isNullOrBlank()) return false
        return addressesMatch(
            StorageRepository.readString(PreferenceKey.SELECTED_DEVICE_ADDRESS),
            address
        )
    }

    /**
     * Fission SDK may emit transient ACL disconnect/reconnect while refreshing GATT.
     * Wallet must not tear down the shared SECORA channel for the host-selected wearable.
     */
    fun shouldIgnoreHostAclDisconnect(address: String?): Boolean = isHostSelectedDeviceAddress(address)

    fun handleIfNeeded(activity: AppCompatActivity, intent: Intent?) {
        ConfiguredWalletIdentity.seedHardcodedIdentity(activity)
        markHostLaunchFromIntent(intent)
        if (intent == null || !intent.getBooleanExtra(EXTRA_PRECONNECTED, false)) {
            return
        }

        val mac = intent.getStringExtra(BundleKey.DEVICE_BLE_ADDRESS)?.trim()?.uppercase(Locale.US).orEmpty()
        if (mac.isEmpty() || !BluetoothAdapter.checkBluetoothAddress(mac)) {
            logger.debug("ExternalLaunch : invalid MAC=$mac")
            return
        }

        val deviceName = intent.getStringExtra(BundleKey.DEVICE_NAME)?.trim().orEmpty()
        StorageRepository.saveString(PreferenceKey.SELECTED_DEVICE_ADDRESS, mac)
        if (deviceName.isNotEmpty()) {
            StorageRepository.saveString(PreferenceKey.DEVICE_NAME, deviceName)
        }
        logger.debug("ExternalLaunch : preconnected device mac=$mac name=$deviceName")

        if (intent.getBooleanExtra(EXTRA_HOST_SECORA_READY, false)
            && BluetoothStateManager.activeProtocol != null
        ) {
            logger.debug("ExternalLaunch : host SECORA protocol already registered, skip payment GATT")
            return
        }

        if (!intent.getBooleanExtra(EXTRA_FISSION_BLE_CONNECTED, false)) {
            logger.debug("ExternalLaunch : host Fission BLE not marked connected, skip payment GATT")
            return
        }

        logger.debug("ExternalLaunch : waiting for host to register shared SECORA protocol")
    }

    fun releaseSharedHostChannel() {
        val protocol = BluetoothStateManager.activeProtocol
        if (protocol is IHostSharedBleProtocol) {
            protocol.releaseSharedChannel()
        }
        BluetoothStateManager.setActiveProtocol(null)
    }

    fun exitToHost(activity: AppCompatActivity) {
        logger.debug("ExternalLaunch : exit to Fission host demo")
        releaseSharedHostChannel()
        StorageRepository.saveBoolean(PreferenceKey.HOST_LAUNCH_ACTIVE, false)
        activity.finish()
    }
}
