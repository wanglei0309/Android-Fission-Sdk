// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT
package com.infineon.secora.wallet.utils.hostedui

import androidx.core.graphics.toColorInt
import com.infineon.secora.wallet.client.data.models.capturecard.ColorConfig
import com.infineon.secora.wallet.client.data.models.capturecard.ErrorPromptConfig
import com.infineon.secora.wallet.client.data.models.capturecard.FieldValidationMessages
import com.infineon.secora.wallet.client.data.models.capturecard.InputFieldConfig
import com.infineon.secora.wallet.client.data.models.capturecard.ScreenConfiguration

/**
 * Description: HostedUILanguage.kt handles the responsibility of parsing language into respective ScreenConfiguration
 * which can be feed during setScreenConfiguration api.
 * This config is used in wallet sdk while displaying hosted ui
 **/
enum class HostedUILanguage {

    ENGLISH,
    FRENCH,
    GERMAN,
    DUTCH,
    SPANISH;

    /**
     * Returns the corresponding [ScreenConfiguration] based on the current enum type.
     *
     * This method maps each supported language type to its respective screen configuration.
     * If the language is not supported, it returns `null`.
     *
     * @return The [ScreenConfiguration] for the selected language, or `null` if no configuration is available.
     */
    fun getScreenConfiguration(): ScreenConfiguration? {
        return when (this) {
            FRENCH -> {
                getFrenchConfig()
            }

            GERMAN -> {
                getGermanConfig()
            }

            DUTCH -> {
                getDutchConfig()
            }

            SPANISH -> {
                getSpanishConfig()
            }

            else -> {
                null
            }
        }
    }

    /**
     * Creates and returns the French-specific [ScreenConfiguration].
     *
     * This configuration includes:
     * - UI color settings tailored for the French locale
     * - Localized input field labels and hints in French
     * - Error prompt messages translated to French
     *
     * @return A fully populated [ScreenConfiguration] configured for French users.
     */
    private fun getFrenchConfig(): ScreenConfiguration {
        val colorConfig = ColorConfig(
            screenBackground = HostedUIConstant.WHITE_COLOR_CODE.value.toColorInt(),
            enabledButtonBackground = HostedUIConstant.FRENCH_ENABLED_HEX.value.toColorInt(),
            disabledButtonBackground = HostedUIConstant.BUTTON_DISABLED_HEX.value.toColorInt(),
            buttonTextColor = HostedUIConstant.WHITE_COLOR_CODE.value.toColorInt()
        )

        val inputFieldConfig = InputFieldConfig(
            cardNumberText = "Numéro de carte",
            cardHolderNameText = "Nom du titulaire tel qu'affiché sur la Carte",
            cardHolderNameHint = "Entrez le nom du titulaire de la carte",
            cardExpiryText = "Valide jusqu'à",
            cardExpiryHint = "MM/AA",
            addCardButtonText = "Ajouter une carte de paiement",
            cancelButtonText = "Annuler"
        )

        val fieldValidationMessages = FieldValidationMessages(
            cardNumberRequired = "Le numéro de carte est obligatoire.",
            cardholderNameRequired = "Le nom du titulaire est obligatoire.",
            expiryDateRequired = "La date d'expiration est obligatoire.",
            cvvRequired = "Le CVV est obligatoire.",
            invalidCvv = "Veuillez saisir un CVV valide à 3 chiffres.",
            invalidExpiry = "Saisir une date d'expiration valide.",
        )
        val errorPromptConfig = ErrorPromptConfig(
            promptLuhnCheckErrorMessage = "Veuillez entrer un numéro de carte valide",
            promptCardSupportedErrorMessage = "Nous acceptons uniquement MasterCard et Visa. Veuillez entrer une carte valide.",
            fieldValidationMessages = fieldValidationMessages
        )

        return ScreenConfiguration(
            colorConfig = colorConfig,
            inputFieldConfig = inputFieldConfig,
            errorPromptConfig = errorPromptConfig
        )
    }

    /**
     * Creates and returns the German-specific [ScreenConfiguration].
     *
     * This configuration includes:
     * - UI color settings tailored for the German locale
     * - Localized input field labels and hints in German
     * - Error prompt messages translated to German
     *
     * @return A fully populated [ScreenConfiguration] configured for German users.
     */
    private fun getGermanConfig(): ScreenConfiguration {
        val colorConfig = ColorConfig(
            screenBackground = HostedUIConstant.WHITE_COLOR_CODE.value.toColorInt(),
            enabledButtonBackground = HostedUIConstant.GERMAN_ENABLED_HEX.value.toColorInt(),
            disabledButtonBackground = HostedUIConstant.BUTTON_DISABLED_HEX.value.toColorInt(),
            buttonTextColor = HostedUIConstant.WHITE_COLOR_CODE.value.toColorInt()
        )
        val inputFieldConfig = InputFieldConfig(
            cardNumberText = "Kartennummer",
            cardHolderNameText = "Name des Karteninhabers wie auf der Karte",
            cardHolderNameHint = "Name des Karteninhabers eingeben",
            cardExpiryText = "Gültig bis",
            cardExpiryHint = "MM/JJ",
            addCardButtonText = "Zahlungskarte hinzufügen",
            cancelButtonText = "Abbrechen"
        )

        val fieldValidationMessages = FieldValidationMessages(
            cardNumberRequired = "Kartennummer ist erforderlich.",
            cardholderNameRequired = "Name des Karteninhabers ist erforderlich.",
            expiryDateRequired = "Ablaufdatum ist erforderlich.",
            cvvRequired = "CVV ist erforderlich.",
            invalidCvv = "Bitte geben Sie einen gültigen 3-stelligen CVV ein.",
            invalidExpiry = "Gültiges Ablaufdatum eingeben.",
        )
        val errorPromptConfig = ErrorPromptConfig(
            promptLuhnCheckErrorMessage = "Bitte eine gültige Kartennummer eingeben.",
            promptCardSupportedErrorMessage = "Wir unterstützen nur MasterCard und Visa. Bitte eine gültige Karte eingeben.",
            fieldValidationMessages = fieldValidationMessages
        )

        return ScreenConfiguration(
            colorConfig = colorConfig,
            inputFieldConfig = inputFieldConfig,
            errorPromptConfig = errorPromptConfig
        )
    }

    /**
     * Creates and returns the Dutch-specific [ScreenConfiguration].
     *
     * This configuration includes:
     * - UI color settings tailored for the Dutch locale
     * - Localized input field labels and hints in Dutch
     * - Error prompt messages translated to Dutch
     *
     * @return A fully populated [ScreenConfiguration] configured for Dutch users.
     */
    private fun getDutchConfig(): ScreenConfiguration {
        val colorConfig = ColorConfig(
            screenBackground = HostedUIConstant.WHITE_COLOR_CODE.value.toColorInt(),
            enabledButtonBackground = HostedUIConstant.DUTCH_ENABLED_HEX.value.toColorInt(),
            disabledButtonBackground = HostedUIConstant.BUTTON_DISABLED_HEX.value.toColorInt(),
            buttonTextColor = HostedUIConstant.WHITE_COLOR_CODE.value.toColorInt()
        )
        val inputFieldConfig = InputFieldConfig(
            cardNumberText = "Kaartnummer",
            cardHolderNameText = "Naam van kaarthouder zoals op de kaart",
            cardHolderNameHint = "Naam van kaarthouder invoeren",
            cardExpiryText = "Geldig tot",
            cardExpiryHint = "MM/JJ",
            addCardButtonText = "Betaalkaart toevoegen",
            cancelButtonText = "Annuleren"
        )
        val fieldValidationMessages = FieldValidationMessages(
            cardNumberRequired = "Kaartnummer is verplicht.",
            cardholderNameRequired = "Naam van de kaarthouder is verplicht.",
            expiryDateRequired = "Vervaldatum is verplicht.",
            cvvRequired = "CVV is verplicht.",
            invalidCvv = "Voer een geldige CVV van 3 cijfers in.",
            invalidExpiry = "Voer een geldige vervaldatum in.",
        )
        val errorPromptConfig = ErrorPromptConfig(
            promptLuhnCheckErrorMessage = "Voer een geldig kaartnummer in.",
            promptCardSupportedErrorMessage = "We ondersteunen alleen MasterCard en Visa. Voer een geldige kaart in.",
            fieldValidationMessages = fieldValidationMessages
        )

        return ScreenConfiguration(
            colorConfig = colorConfig,
            inputFieldConfig = inputFieldConfig,
            errorPromptConfig = errorPromptConfig
        )
    }

    /**
     * Creates and returns the Spanish-specific [ScreenConfiguration].
     *
     * This configuration includes:
     * - UI color settings tailored for the Spanish locale
     * - Localized input field labels and hints in Spanish
     * - Error prompt messages translated to Spanish
     *
     * @return A fully populated [ScreenConfiguration] configured for Spanish users.
     */
    private fun getSpanishConfig(): ScreenConfiguration {
        val colorConfig = ColorConfig(
            screenBackground = HostedUIConstant.WHITE_COLOR_CODE.value.toColorInt(),
            enabledButtonBackground = HostedUIConstant.SPANISH_ENABLED_HEX.value.toColorInt(),
            disabledButtonBackground = HostedUIConstant.BUTTON_DISABLED_HEX.value.toColorInt(),
            buttonTextColor = HostedUIConstant.WHITE_COLOR_CODE.value.toColorInt()
        )
        val inputFieldConfig = InputFieldConfig(
            cardNumberText = "Número de tarjeta",
            cardHolderNameText = "Nombre del titular como aparece en la tarjeta",
            cardHolderNameHint = "Introducir nombre del titular",
            cardExpiryText = "Válida hasta",
            cardExpiryHint = "MM/AA",
            addCardButtonText = "Añadir tarjeta de pago",
            cancelButtonText = "Cancelar"
        )

        val fieldValidationMessages = FieldValidationMessages(
            cardNumberRequired = "El número de tarjeta es obligatorio.",
            cardholderNameRequired = "El nombre del titular es obligatorio.",
            expiryDateRequired = "La fecha de caducidad es obligatoria.",
            cvvRequired = "El CVV es obligatorio.",
            invalidCvv = "Introduce un CVV válido de 3 dígitos.",
            invalidExpiry = "Introduce una fecha de caducidad válida.",
        )
        val errorPromptConfig = ErrorPromptConfig(
            promptLuhnCheckErrorMessage = "Introduce un número de tarjeta válido.",
            promptCardSupportedErrorMessage = "Solo admitimos MasterCard y Visa. Introduce una tarjeta válida.",
            fieldValidationMessages = fieldValidationMessages
        )

        return ScreenConfiguration(
            colorConfig = colorConfig,
            inputFieldConfig = inputFieldConfig,
            errorPromptConfig = errorPromptConfig
        )
    }
}