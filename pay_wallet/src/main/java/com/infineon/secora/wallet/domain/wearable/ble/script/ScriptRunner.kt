// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: ScriptRunner.kt acts as a wrapper around the SecoraWearableSDK, handling secure element script operations
 * such as execution, deletion, SEID fetching, and PPSE processing. It provides asynchronous methods using
 * CompletableFuture to ensure smooth, non-blocking execution of BLE-based secure element tasks.
 **/
package com.infineon.secora.wallet.domain.wearable.ble.script

import android.content.Context
import com.infineon.secora.wallet.logger.ApplicationLogger
import com.infineon.secora.wallet.logger.ApplicationLogger.Companion.getApplicationLogger
import com.infineon.secora.wearable.SecoraWearableSDK
import com.infineon.secora.wearable.util.CPLCData
import com.infineon.secora.wearable.util.CasdCertificates
import com.infineon.secora.wearable.apducdcvm.SetPuk
import com.infineon.secora.wearable.protocolapi.IAsyncProtocol
import com.infineon.secora.wearable.scriptloader.JsonScriptLoader
import com.infineon.secora.wearable.scriptloader.UseCaseDataProvider
import java.util.concurrent.CompletableFuture

/**
 * ScriptRunner executes a JSON script for the card operations.
 */
class ScriptRunner {
    private val logger: ApplicationLogger = getApplicationLogger(this::class.java.simpleName)

    /**
     * Default constructor
     * Initialize SetPuk with default data provider
     */
    init {
        SetPuk(UseCaseDataProvider.getInstance().puk())
    }

    /**
     * Fetches SEID from device using CPLC script.
     * This method calls the SecoraWearableSDK to handle the entire CPLC flow.
     *
     * @param context  The application context for asset access
     * @param protocol The async protocol instance
     * @return CompletableFuture that completes with SEID string
     */
    fun fetchCPLCData(
        context: Context,
        protocol: IAsyncProtocol
    ): CompletableFuture<CPLCData> {
        logger.debug("Starting fetchCPLCData")
        return SecoraWearableSDK.getInstance().getInterface().fetchCPLCData(context, protocol)
    }

    /**
     * Reads MDES and VTS CASD certificates from the secure element.
     *
     * @param context The application context for asset access.
     * @param protocol The async protocol instance connected to the wearable.
     * @return CompletableFuture that completes with parsed [CasdCertificates].
     */
    fun fetchCasdCertificates(
        context: Context,
        protocol: IAsyncProtocol
    ): CompletableFuture<CasdCertificates> {
        logger.debug("Starting fetchCasdCertificates")
        return SecoraWearableSDK.getInstance().getInterface().fetchCasdCertificates(context, protocol)
    }

    /**
     * Executes PPSE script for MCM operations.
     * This method calls the SecoraWearableSDK to handle the entire PPSE flow.
     *
     * @param context      The application context for asset access
     * @param protocol     The async protocol instance
     * @param ppseFileName The logical name of the PPSE file (e.g., "PPSE-01", "PPSE-02", "PPSE-03")
     * @return CompletableFuture that completes with execution success boolean
     */
    fun executePPSEScript(
        context: Context,
        protocol: IAsyncProtocol,
        aid: String,
        cardType: String,
        otherCardAppletInstanceAids: List<String> = emptyList()
    ): CompletableFuture<Boolean> {
        logger.debug("Starting executePPSEScript for:")
        return SecoraWearableSDK.getInstance().getInterface()
            .executePPSEScript(context, protocol, aid, cardType, otherCardAppletInstanceAids)
    }

    /**
     * Executes script for card operations and returns detailed results with apduCommandId and apduCommandResponse.
     * This method calls the SecoraWearableSDK to handle the entire script execution flow
     * and returns detailed results for each APDU command executed.
     *
     * @param isDeleteScriptExecution  The flag confirms if this is a delete script execution
     * @param protocol  The async protocol instance
     * @param jsonBytes The JSON script bytes to execute
     * @return CompletableFuture that completes with list of ApduExecutionResult containing apduCommandId and apduCommandResponse
     */
    fun executeScript(
        isDeleteScriptExecution: Boolean,
        protocol: IAsyncProtocol,
        jsonBytes: ByteArray
    ): CompletableFuture<List<JsonScriptLoader.ApduExecutionResult>> {
        return executeScript(
            isDeleteScriptExecution = isDeleteScriptExecution,
            clearDefaultCard = false,
            protocol = protocol,
            jsonBytes = jsonBytes
        )
    }

    /**
     * Executes script for card operations, optionally clearing the default card after delete.
     *
     * @param isDeleteScriptExecution Whether this is a delete script execution
     * @param clearDefaultCard When true with delete, run clear-default APDU after delete
     * @param protocol The async protocol instance
     * @param jsonBytes The JSON script bytes to execute
     */
    fun executeScript(
        isDeleteScriptExecution: Boolean,
        clearDefaultCard: Boolean,
        protocol: IAsyncProtocol,
        jsonBytes: ByteArray
    ): CompletableFuture<List<JsonScriptLoader.ApduExecutionResult>> {
        logger.debug(
            "Starting executeScript, isDeleteScriptExecution : $isDeleteScriptExecution, " +
                "clearDefaultCard : $clearDefaultCard"
        )
        return SecoraWearableSDK.getInstance().getInterface()
            .executeScript(isDeleteScriptExecution, clearDefaultCard, protocol, jsonBytes)
    }
}
