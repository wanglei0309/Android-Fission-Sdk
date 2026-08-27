// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: ImageUtils.kt is a helper object providing common utility functions for image bitmap conversion
 **/
package com.infineon.secora.wallet.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream

object ImageUtils {

    fun base64ToBitmap(base64Image: String): Bitmap {
        val bDecode = Base64.decode(base64Image, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(
            bDecode,
            0,
            bDecode.size
        );
    }

    fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(
            Bitmap.CompressFormat.PNG,
            100,
            outputStream
        )

        return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
    }
}