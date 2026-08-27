// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

package com.infineon.secora.wallet

import com.infineon.secora.wallet.logger.ApplicationLogger.Companion.getApplicationLogger

/**
 * Host（Fission App）与手表之间的功能开关 FSS 同步桥接。
 *
 * 英飞凌支付添卡流程：
 * - 用户点击「+」添卡时：AT+FSS:62,1，延时 50ms 后继续原流程
 * - 添卡完成：AT+FSS:63,1
 * - 添卡失败：AT+FSS:64,1
 *
 * 具体 AT 发送由宿主通过 [Delegate] 实现（通常为 [FissionSdkBleManage]）。
 */
object PayHostFssSync {

    private val logger = getApplicationLogger("PayHostFssSync")

    @Volatile
    private var addCardSessionActive = false

    interface Delegate {
        fun notifyInfineonPayAddCardStarting()
        fun notifyInfineonPayAddCardSuccess()
        fun notifyInfineonPayAddCardFailed()
    }

    @Volatile
    var delegate: Delegate? = null

    /** 在用户点击「+」添卡时调用（每个添卡会话一次，发送 AT+FSS:62,1）。 */
    @JvmStatic
    fun onAddCardStarting() {
        if (addCardSessionActive) {
            return
        }
        addCardSessionActive = true
        val host = delegate
        if (host == null) {
            logger.debug("onAddCardStarting: no host delegate registered")
            return
        }
        logger.debug("onAddCardStarting: FSS f=62")
        host.notifyInfineonPayAddCardStarting()
    }

    /** 发卡全流程成功结束（notify-provision 成功并进入成功 UI）时调用。 */
    @JvmStatic
    fun onAddCardSuccess() {
        if (!addCardSessionActive) {
            return
        }
        addCardSessionActive = false
        val host = delegate
        if (host == null) {
            logger.debug("onAddCardSuccess: no host delegate registered")
            return
        }
        logger.debug("onAddCardSuccess: FSS f=63")
        host.notifyInfineonPayAddCardSuccess()
    }

    /** 添卡流程失败结束时调用。 */
    @JvmStatic
    fun onAddCardFailed() {
        if (!addCardSessionActive) {
            return
        }
        addCardSessionActive = false
        val host = delegate
        if (host == null) {
            logger.debug("onAddCardFailed: no host delegate registered")
            return
        }
        logger.debug("onAddCardFailed: FSS f=64")
        host.notifyInfineonPayAddCardFailed()
    }

    /** 放弃当前添卡会话且不发送结果 FSS（未发送 f=62 时）。 */
    @JvmStatic
    fun resetAddCardSession() {
        addCardSessionActive = false
    }
}
