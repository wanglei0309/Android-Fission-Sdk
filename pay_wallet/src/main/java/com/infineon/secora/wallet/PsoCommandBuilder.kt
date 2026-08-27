package com.infineon.secora.wallet

import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.security.KeyPair
import java.security.PrivateKey
import java.security.Signature
import java.security.interfaces.ECPublicKey

/**
 * Generic BER-TLV encoder (minimal-form lengths per Table 14 of the CDCVM v3 Applet
 * Administration Guide).
 */
object Tlv {
    fun tag(tagValue: Long): ByteArray {
        var v = tagValue
        val bytes = ByteArrayOutputStream()
        var started = false
        for (shift in intArrayOf(24, 16, 8, 0)) {
            val b = ((v ushr shift) and 0xFF).toInt()
            if (b != 0 || started) {
                bytes.write(b)
                started = true
            }
        }
        if (!started) bytes.write(0)
        return bytes.toByteArray()
    }

    fun length(len: Int): ByteArray {
        if (len < 0x80) return byteArrayOf(len.toByte())
        var l = len
        val tmp = ArrayDeque<Byte>()
        while (l > 0) { tmp.addFirst((l and 0xFF).toByte()); l = l shr 8 }
        return byteArrayOf((0x80 or tmp.size).toByte()) + tmp.toByteArray()
    }

    fun build(tagValue: Long, value: ByteArray): ByteArray = tag(tagValue) + length(value.size) + value
    fun build(tagValue: Long, values: List<ByteArray>): ByteArray = build(tagValue, values.reduce { a, b -> a + b })
}

fun ByteArray.toHex(): String = joinToString(" ") { "%02X".format(it) }

/** Packs an 8-digit "YYYYMMDD" string into 4-byte BCD, exactly as Table 61 requires for 5F24/5F25. */
fun encodeDateBcd(yyyymmdd: String): ByteArray {
    require(yyyymmdd.length == 8 && yyyymmdd.all { it.isDigit() }) {
        "Expected an 8-digit YYYYMMDD string, got: $yyyymmdd"
    }
    return ByteArray(4) { i ->
        val hi = yyyymmdd[i * 2] - '0'
        val lo = yyyymmdd[i * 2 + 1] - '0'
        ((hi shl 4) or lo).toByte()
    }
}

/** Key usage values from Table 61 — tag 95's length AND value both depend on which cert this is. */
enum class KeyUsage(val valueBytes: ByteArray) {
    /** CERT.KA-KLOC.ECDSA: length '01', value '82' (Digital signature verification) */
    CA_KLOC_ECDSA(byteArrayOf(0x82.toByte())),
    /** CERT.OCE.ECKA: length '02', value '0080' (Key agreement) */
    OCE_ECKA(byteArrayOf(0x00, 0x80.toByte()))
}

/** Everything needed to build one certificate per Table 61 (excluding the signature, computed separately). */
data class CertificateSpec(
    val serialNumber: ByteArray,         // 93, 1-16 bytes
    val caKlocIdentifier: String,        // 42 (CAR) - must equal previous cert's subjectIdentifier in a chain
    val subjectIdentifier: String,       // 5F20 (CHR)
    val keyUsage: KeyUsage,              // 95
    val expirationDateYYYYMMDD: String,  // 5F24 - mandatory, BCD-encoded automatically
    val effectiveDateYYYYMMDD: String? = null, // 5F25 - optional, BCD-encoded automatically
    val subjectPublicKey: ECPublicKey,
    val coordSizeBytes: Int = 32         // 32 for P-256
)

object CvCertificateBuilder {

    /** 04 || X || Y, each coordinate fixed to coordSizeBytes (32 for P-256) per Table 61's "uncompressed encoding". */
    fun ecPublicKeyToPoint(publicKey: ECPublicKey, coordSizeBytes: Int = 32): ByteArray {
        val w = publicKey.w
        return byteArrayOf(0x04) +
            bigIntToFixedBytes(w.affineX, coordSizeBytes) +
            bigIntToFixedBytes(w.affineY, coordSizeBytes)
    }

    private fun bigIntToFixedBytes(value: BigInteger, size: Int): ByteArray {
        val raw = value.toByteArray()
        val trimmed = if (raw.size > size) raw.copyOfRange(raw.size - size, raw.size) else raw
        return if (trimmed.size == size) trimmed else ByteArray(size - trimmed.size) + trimmed
    }

    /** DER (SEQUENCE{INTEGER r, INTEGER s}) -> fixed-length raw r||s, as Table 61 expects for 5F37. */
    fun derSignatureToRaw(der: ByteArray, coordSizeBytes: Int = 32): ByteArray {
        var offset = 0
        require(der[offset] == 0x30.toByte()) { "Not a DER SEQUENCE" }
        offset++
        var seqLen = der[offset].toInt() and 0xFF; offset++
        if (seqLen and 0x80 != 0) {
            val n = seqLen and 0x7F; seqLen = 0
            repeat(n) { seqLen = (seqLen shl 8) or (der[offset].toInt() and 0xFF); offset++ }
        }
        require(der[offset] == 0x02.toByte()); offset++
        val rLen = der[offset].toInt() and 0xFF; offset++
        val r = der.copyOfRange(offset, offset + rLen); offset += rLen
        require(der[offset] == 0x02.toByte()); offset++
        val sLen = der[offset].toInt() and 0xFF; offset++
        val s = der.copyOfRange(offset, offset + sLen)

        fun normalize(c: ByteArray): ByteArray {
            val trimmed = if (c.size > 1 && c[0] == 0.toByte()) c.copyOfRange(1, c.size) else c
            return if (trimmed.size >= coordSizeBytes) trimmed.copyOfRange(trimmed.size - coordSizeBytes, trimmed.size)
            else ByteArray(coordSizeBytes - trimmed.size) + trimmed
        }
        return normalize(r) + normalize(s)
    }

    fun signRaw(data: ByteArray, privateKey: PrivateKey, coordSizeBytes: Int = 32): ByteArray {
        val sig = Signature.getInstance("SHA256withECDSA")
        sig.initSign(privateKey)
        sig.update(data)
        return derSignatureToRaw(sig.sign(), coordSizeBytes)
    }

    /**
     * Builds one complete certificate (7F21 template) per Table 61, field order exactly as specified,
     * signed with [issuerPrivateKey] (the CA/previous-cert key — NOT this cert's own subject key).
     */
    fun buildCertificate(spec: CertificateSpec, issuerPrivateKey: PrivateKey): ByteArray {
        val tag93 = Tlv.build(0x93, spec.serialNumber)
        val tag42 = Tlv.build(0x42, spec.caKlocIdentifier.toByteArray(Charsets.US_ASCII))
        val tag5F20 = Tlv.build(0x5F20, spec.subjectIdentifier.toByteArray(Charsets.US_ASCII))
        val tag95 = Tlv.build(0x95, spec.keyUsage.valueBytes)
        val tag5F25 = spec.effectiveDateYYYYMMDD?.let { Tlv.build(0x5F25, encodeDateBcd(it)) } ?: ByteArray(0)
        val tag5F24 = Tlv.build(0x5F24, encodeDateBcd(spec.expirationDateYYYYMMDD))

        val ecPoint = ecPublicKeyToPoint(spec.subjectPublicKey, spec.coordSizeBytes)
        val tagB0 = Tlv.build(0xB0, ecPoint)
        val tagF0 = Tlv.build(0xF0, byteArrayOf(0x00)) // '00' for P-256, per Table 61
        val tag7F49 = Tlv.build(0x7F49, listOf(tagB0, tagF0))

        // Table 61: "signature is generated over the TLVs preceding the Signature TLV,
        // that is, from the certificate serial number to the public key data object"
        val tbs = tag93 + tag42 + tag5F20 + tag95 + tag5F25 + tag5F24 + tag7F49
        val signature = signRaw(tbs, issuerPrivateKey, spec.coordSizeBytes)
        val tag5F37 = Tlv.build(0x5F37, signature)

        return Tlv.build(0x7F21, tbs + tag5F37)
    }

    /**
     * Builds a full certificate chain: zero or more CERT.KA-KLOC.ECDSA followed by exactly one
     * CERT.OCE.ECKA, each cert signed by the previous one's private key (the first is signed by
     * the CA root key you already hold, [rootIssuerPrivateKey]).
     *
     * @param entries ordered list of (spec, subjectKeyPair) — subjectKeyPair.private signs the
     *                *next* entry in the chain; it is never used to sign its own certificate.
     * @return list of complete 7F21 certificate byte arrays, in the same order as [entries]
     *         (intermediate CERT.KA-KLOC.ECDSA certs first, CERT.OCE.ECKA last).
     */
    fun buildChain(rootIssuerPrivateKey: PrivateKey, entries: List<Pair<CertificateSpec, KeyPair>>): List<ByteArray> {
        require(entries.isNotEmpty()) { "Chain must contain at least the CERT.OCE.ECKA" }
        require(entries.last().first.keyUsage == KeyUsage.OCE_ECKA) { "Last entry must be CERT.OCE.ECKA" }
        entries.dropLast(1).forEach {
            require(it.first.keyUsage == KeyUsage.CA_KLOC_ECDSA) { "All entries but the last must be CERT.KA-KLOC.ECDSA" }
        }

        val certs = mutableListOf<ByteArray>()
        var signingKey = rootIssuerPrivateKey
        for ((spec, keyPair) in entries) {
            certs += buildCertificate(spec, signingKey)
            signingKey = keyPair.private // this entry's key signs the NEXT one
        }
        return certs
    }
}

/**
 * Builds the PERFORM SECURITY OPERATION command APDU(s) per Table 57-60. One certificate per
 * APDU (each cert here is well under the 65535-byte extended-length limit, so no intra-certificate
 * fragmentation is needed — only certificate-to-certificate chaining via P2 bit 8).
 */
object PsoCommandBuilder {

    /**
     * @param certificates ordered 7F21 byte arrays: 0..n-1 CERT.KA-KLOC.ECDSA, then CERT.OCE.ECKA last
     * @param keyVersionNumber key version number of PK.CA-KLOC.ECDSA (P1 bits 1-7) — only meaningful
     *                         for the first certificate; per the SW table, must match a version the
     *                         device actually has provisioned
     * @param keyIdentifier key identifier of PK.CA-KLOC.ECDSA (P2 bits 1-7) — only meaningful for the
     *                      first certificate; per the status-word table, the device requires this to
     *                      equal 0x10 for the first certificate, or it returns 6A88
     * @param cla '80' or '84' per Table 57
     */
    fun buildPsoApdus(
        certificates: List<ByteArray>,
        keyVersionNumber: Int,
        keyIdentifier: Int = 0x10,
        cla: Byte = 0x80.toByte()
    ): List<ByteArray> {
        require(certificates.isNotEmpty())
        return certificates.mapIndexed { index, certBytes ->
            val isFirst = index == 0
            val isLast = index == certificates.lastIndex

            // P1 bit 8: "more blocks of the currently submitted certificate" -- always 0 here since
            // each certificate fits in a single (extended) APDU. Bits 1-7: key version number,
            // meaningful only for the first certificate.
            val p1 = ((if (isFirst) keyVersionNumber else 0) and 0x7F).toByte()

            // P2 bit 8: "more certificates follow" (1) vs "this is the last / CERT.OCE.ECKA" (0).
            // Bits 1-7: key identifier, meaningful only for the first certificate.
            val moreCertsBit = if (isLast) 0x00 else 0x80
            val p2 = (moreCertsBit or ((if (isFirst) keyIdentifier else 0) and 0x7F)).toByte()

            buildSinglePsoApdu(cla, p1, p2, certBytes)
        }
    }

    private fun buildSinglePsoApdu(cla: Byte, p1: Byte, p2: Byte, data: ByteArray): ByteArray {
        val ins = 0x2A.toByte()
        val useExtended = data.size > 0xFF
        val header = byteArrayOf(cla, ins, p1, p2)
        val lc = if (useExtended) {
            byteArrayOf(0x00, ((data.size shr 8) and 0xFF).toByte(), (data.size and 0xFF).toByte())
        } else {
            byteArrayOf(data.size.toByte())
        }
        // Table 57 requires Le='00' always. Once Lc uses extended form, Le must also be encoded in
        // extended (2-byte) form -- a single trailing 0x00 byte is invalid and will fail to parse.
        val le = if (useExtended) byteArrayOf(0x00, 0x00) else byteArrayOf(0x00)
        return header + lc + data + le
    }
}

fun main() {
    // --- Example: build a 2-certificate chain (CERT.KA-KLOC.ECDSA -> CERT.OCE.ECKA) and its PSO APDUs ---

    // In real usage: rootCaKeyPair.private is SK.CA-KLOC.ECDSA (held by your backend/HSM, NOT generated
    // fresh on-device), and each subject key pair is generated wherever that key actually needs to live.
    val kpg = java.security.KeyPairGenerator.getInstance("EC")
    kpg.initialize(java.security.spec.ECGenParameterSpec("secp256r1"))
    val rootCaKeyPair = kpg.generateKeyPair()
    val klocKeyPair = kpg.generateKeyPair()
    val oceKeyPair = kpg.generateKeyPair()

    val klocSpec = CertificateSpec(
        serialNumber = byteArrayOf(0x00, 0x01),
        caKlocIdentifier = "IFX CA-KLOC 0001",
        subjectIdentifier = "IFX KA-KLOC 0001",
        keyUsage = KeyUsage.CA_KLOC_ECDSA,
        expirationDateYYYYMMDD = "20301231",
        subjectPublicKey = klocKeyPair.public as ECPublicKey
    )

    val oceSpec = CertificateSpec(
        serialNumber = byteArrayOf(0x00, 0x01),
        caKlocIdentifier = "IFX KA-KLOC 0001", // must equal the previous cert's subjectIdentifier
        subjectIdentifier = "IFX OCE-ECKA",
        keyUsage = KeyUsage.OCE_ECKA,
        expirationDateYYYYMMDD = "20301231",
        subjectPublicKey = oceKeyPair.public as ECPublicKey
    )

    val certChain = CvCertificateBuilder.buildChain(
        rootIssuerPrivateKey = rootCaKeyPair.private,
        entries = listOf(klocSpec to klocKeyPair, oceSpec to oceKeyPair)
    )

    // Key identifier '0x10' is required by the device for the first certificate (Table 63: 6A88 otherwise).
    // Key version number must match whatever PK.CA-KLOC.ECDSA version is actually provisioned on the device.
    val apdus = PsoCommandBuilder.buildPsoApdus(
        certificates = certChain,
        keyVersionNumber = 0x1A,
        keyIdentifier = 0x10
    )

    apdus.forEachIndexed { i, apdu ->
        println("APDU #$i (${apdu.size} bytes): ${apdu.toHex()}")
    }
}
