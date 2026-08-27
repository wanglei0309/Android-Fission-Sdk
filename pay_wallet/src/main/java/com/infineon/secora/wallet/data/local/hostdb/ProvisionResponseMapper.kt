// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

package com.infineon.secora.wallet.data.local.hostdb

import com.infineon.secora.wallet.client.data.models.card.common.CardDetails
import com.infineon.secora.wallet.client.data.models.common.CardList
import com.infineon.secora.wallet.utils.constants.Constants.ACTIVE
import com.infineon.secora.wallet.utils.constants.Constants.CONTENT_TYPE_DIGITAL_CARD_ART
import com.infineon.secora.wallet.utils.constants.Constants.INACTIVE
import com.infineon.secora.wallet.utils.constants.Constants.PNO_MDES
import com.infineon.secora.wallet.utils.constants.Constants.PNO_VTS

object ProvisionResponseMapper {

    /**
     * Maps one provision API card row into the app-facing [CardDetails] model.
     *
     * Per-card [CardList.panUniqueReference] is preferred when present. Optional session fallbacks
     * come from app-owned preferences,
     * populated by the app when those values are known (e.g. after digitize).
     *
     * @param serverCard Card payload received from the provision API.
     * @param sessionPanUniqueReference Session-level PAN unique reference when the row has none.
     * @return Mapped [CardDetails] used by app UI and host cache.
     */
    fun cardListToCardDetails(
        serverCard: CardList,
        sessionPanUniqueReference: String = ""
    ): CardDetails {
        val rowPanUniqueRef = serverCard.panUniqueReference?.trim().orEmpty()
        val panUniqueReference = rowPanUniqueRef.ifEmpty { sessionPanUniqueReference.trim() }

        val card = CardDetails()
        card.digitizationReferenceNumber = serverCard.digitizationReferenceNumber
        card.ppseFile = serverCard.ppseFileName
        card.cardStatus = serverCard.status
        card.pnoType = serverCard.pnoType
        card.serviceUrl = INACTIVE
        card.privacyPolicyURL = INACTIVE
        card.bankName = INACTIVE
        card.cardAddedStatus = ACTIVE
        card.tacAssetId = serverCard.productConfig?.iconAssetId
        if (serverCard.pnoType.equals(PNO_MDES)) {
            card.cardAssetId = serverCard.productConfig?.cardBackgroundCombinedAssetId
            card.foreGroundColor = serverCard.productConfig?.foregroundColor
            card.backGroundColor = serverCard.productConfig?.backgroundColor
            card.labelColor = serverCard.productConfig?.labelColor
        } else if (serverCard.pnoType.equals(PNO_VTS)) {
            card.foreGroundColor = serverCard.cardMetaData?.foregroundColor
            card.backGroundColor = serverCard.cardMetaData?.backgroundColor
            card.labelColor = serverCard.cardMetaData?.labelColor
            val guid = serverCard.cardMetaData?.cardData
                ?.firstOrNull { it.contentType == CONTENT_TYPE_DIGITAL_CARD_ART }
                ?.guid
            card.cardAssetId = guid
        }
        card.tokenNumber = serverCard.tokenInfo?.tokenPanSuffix
        card.txnAuthCode = INACTIVE
        card.refreshHistory = ACTIVE
        card.encryptedData = ""
        card.isDefault = INACTIVE
        card.dpanSuffix = serverCard.dpanSuffix
        card.paymentAppInstanceId = serverCard.paymentAppInstanceId
        card.cardExpiry = serverCard.expirationDate
        card.cardDecision = serverCard.cardDecision
        card.panUniqueReference = panUniqueReference
        card.customUrl = serverCard.productConfig?.customerServiceUrl
        card.contactNumber = serverCard.cardMetaData?.contactNumber
        card.privacyPolicyURL = serverCard.cardMetaData?.privacyPolicyURL
        card.contactWebsite = serverCard.cardMetaData?.contactWebsite
        card.termsAndConditionsURL = serverCard.cardMetaData?.termsAndConditionsURL
        card.contactEmail = serverCard.cardMetaData?.contactEmail
        card.expDatePrintedInd = serverCard.tokenInfo?.expDatePrintedInd
        card.authenticationMethods = serverCard.authenticationMethods
        if (serverCard.pnoType.equals(PNO_MDES)) {
            card.contactEmail = serverCard.productConfig?.customerServiceEmail
            card.contactNumber = serverCard.productConfig?.customerServicePhoneNumber
            card.contactWebsite = serverCard.productConfig?.customerServiceUrl
            card.termsAndConditionsURL = serverCard.productConfig?.termsAndConditionsUrl
            card.privacyPolicyURL = serverCard.productConfig?.privacyPolicyURL
        }
        card.cardNickname = ""
        return card
    }

    /**
     * Converts app card cache representation back into status API list payload format.
     *
     * @param details Card details from host cache.
     * @return API-facing [CardList] representation.
     */
    fun cardDetailsToCardList(details: CardDetails): CardList {
        return CardList(
            dpanSuffix = details.dpanSuffix,
            expirationDate = details.cardExpiry,
            digitizationReferenceNumber = details.digitizationReferenceNumber,
            ppseFileName = details.ppseFile,
            status = details.cardStatus,
            pnoType = details.pnoType,
            cardDecision = details.cardDecision,
            paymentAppInstanceId = details.paymentAppInstanceId,
            panUniqueReference = details.panUniqueReference
        )
    }
}
