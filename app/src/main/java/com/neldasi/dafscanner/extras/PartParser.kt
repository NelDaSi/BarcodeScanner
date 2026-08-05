package com.neldasi.dafscanner.extras

import java.util.Locale

enum class EngineFormat {
    MX11,
    MX13,
    P14,
    UNKNOWN,
}

data class ParsedPart(
    val rawCode: String,
    val format: EngineFormat,
    val typeCode: String,
    val supplierCode: String,
    val serialHex: String,
    val serialDecimal: String,
    val batchNumber: String = "",
) {
    // Legacy support for other parts of the app
    val serialNumber: String get() = serialHex
    val decSerial: String get() = serialDecimal
}

fun parseScannedCode(code: String): ParsedPart? {
    val trimmedCode = code.trim()
    if (trimmedCode.length < 18) return null

    return try {
        val type = trimmedCode.substring(0, 7)
        val supplier = trimmedCode.substring(7, 12)

        when {
            // 1. MX11: Contains 'K'
            trimmedCode.contains("K") -> {
                val hex = trimmedCode.substring(12, 18).uppercase(Locale.ROOT)
                val decimal = hex.toLong(16).toString()
                ParsedPart(
                    rawCode = trimmedCode,
                    format = EngineFormat.MX11,
                    typeCode = type,
                    supplierCode = supplier,
                    serialHex = hex,
                    serialDecimal = decimal,
                    batchNumber = trimmedCode.substring(18),
                )
            }

            // 2. P14: Newest format. Priority for 27 chars + Numeric only.
            (trimmedCode.length == 27) && trimmedCode.all { it.isDigit() } -> {
                val decimalStr = trimmedCode.substring(trimmedCode.length - 7)
                val decimalVal = decimalStr.toLong()
                val hex = decimalVal.toString(16).uppercase(Locale.ROOT).padStart(6, '0')
                
                ParsedPart(
                    rawCode = trimmedCode,
                    format = EngineFormat.P14,
                    typeCode = type,
                    supplierCode = supplier,
                    serialHex = hex,
                    serialDecimal = decimalStr,
                    batchNumber = trimmedCode.substring(12, 20),
                )
            }

            // 3. MX13: Contains "0074" marker logic or older 29-char structure
            (trimmedCode.indexOf("0074", 12) != -1) || (trimmedCode.length >= 29) -> {
                val hex = trimmedCode.substring(12, 18).uppercase(Locale.ROOT)
                val decimal = try {
                    val markerIndex = trimmedCode.indexOf("0074", 18)
                    if (markerIndex != -1) {
                        trimmedCode.substring(markerIndex + 4).trimStart('0').ifBlank { "0" }
                    } else {
                        hex.toLong(16).toString()
                    }
                } catch (_: Exception) {
                    hex.toLong(16).toString()
                }

                ParsedPart(
                    rawCode = trimmedCode,
                    format = EngineFormat.MX13,
                    typeCode = type,
                    supplierCode = supplier,
                    serialHex = hex,
                    serialDecimal = decimal,
                    batchNumber = "",
                )
            }

            // Fallback for codes that don't strictly match but are >= 18
            else -> {
                val hex = trimmedCode.substring(12, 18).uppercase(Locale.ROOT)
                val decimal = hexToDecimalOrNA(hex)
                ParsedPart(
                    rawCode = trimmedCode,
                    format = EngineFormat.UNKNOWN,
                    typeCode = type,
                    supplierCode = supplier,
                    serialHex = hex,
                    serialDecimal = decimal,
                    batchNumber = if (trimmedCode.length > 18) trimmedCode.substring(18) else "",
                )
            }
        }
    } catch (_: Exception) {
        null
    }
}

internal fun hexToDecimalOrNA(hex: String): String {
    return try {
        hex.toLong(16).toString()
    } catch (_: Exception) {
        "N/A"
    }
}
