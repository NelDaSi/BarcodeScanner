package com.neldasi.dafscanner.extras

import com.neldasi.dafscanner.data.SearchItem

// Extracted from SearchListViewModel.loadCsv so it can be unit tested without an Android context.
fun parseSearchItemsCsv(allText: String): List<SearchItem> {
    if (allText.isBlank()) return emptyList()

    val rows = tokenizeCsv(allText)
    if (rows.isEmpty()) return emptyList()

    var productIdIndex = -1
    var machineIndex = -1
    var outputMatIndex = -1
    var startDateIndex = -1
    var startTimeIndex = -1
    var completeDateIndex = -1
    var completeTimeIndex = -1
    var headerRowIndex = -1

    fun String.normalize() = this.replace("\n", " ").replace("\r", " ").trim().lowercase()

    for (rowIdx in 0 until minOf(rows.size, 100)) {
        val row = rows[rowIdx]
        val normalized = row.map { it.normalize() }

        // Look for Product ID or Transport Number
        val pIdIdx = normalized.indexOfFirst {
            it.contains("product id") || it.contains("transport number") || it.contains("part id")
        }

        if (pIdIdx != -1) {
            headerRowIndex = rowIdx
            productIdIndex = pIdIdx
            machineIndex = normalized.indexOfFirst { (it == "machine") || (it == "workstation") || (it == "station") }
            outputMatIndex = normalized.indexOfFirst { (it == "output material") || (it == "model") || (it == "material") }
            startDateIndex = normalized.indexOfFirst { it.contains("start date") }
            startTimeIndex = normalized.indexOfFirst { it.contains("start time") }
            completeDateIndex = normalized.indexOfFirst { it.contains("complete date") }
            completeTimeIndex = normalized.indexOfFirst { it.contains("complete time") }
            break
        }
    }

    val itemsList = mutableListOf<SearchItem>()
    val processedSerials = mutableSetOf<String>()
    val startIdx = if (headerRowIndex != -1) headerRowIndex + 1 else 1

    for (r in startIdx until rows.size) {
        val row = rows[r]
        val pIdIdx = productIdIndex
        if ((pIdIdx != -1) && (row.size > pIdIdx)) {
            val productId = row[pIdIdx]
            if (productId.isNotBlank() && (productId.length >= 18)) {
                val parsed = parseScannedCode(productId)
                val hex = parsed?.serialHex ?: productId.takeLast(6)

                if (!processedSerials.contains(hex)) {
                    val type = parsed?.typeCode ?: productId.take(7)

                    itemsList.add(
                        SearchItem(
                            typeCode = type,
                            serialNumber = hex,
                            decSerial = parsed?.serialDecimal ?: hexToDecimalOrNA(hex),
                            machine = csvValue(row, machineIndex),
                            outputMaterial = csvValue(row, outputMatIndex),
                            startDate = csvValue(row, startDateIndex),
                            startTime = csvValue(row, startTimeIndex),
                            completeDate = csvValue(row, completeDateIndex),
                            completeTime = csvValue(row, completeTimeIndex),
                        ),
                    )
                    processedSerials.add(hex)
                }
            }
        }
    }
    return itemsList
}

private fun tokenizeCsv(allText: String): List<List<String>> {
    val rows = mutableListOf<List<String>>()
    val currentRow = mutableListOf<String>()
    val currentField = StringBuilder()
    var inQuotes = false

    val firstLine = allText.lineSequence().firstOrNull() ?: ""
    val delimiter = if (firstLine.contains(";")) ';' else ','

    var i = 0
    while (i < allText.length) {
        val c = allText[i]
        if (inQuotes) {
            if (c == '"') {
                if (((i + 1) < allText.length) && (allText[i + 1] == '"')) {
                    currentField.append('"')
                    i++
                } else {
                    inQuotes = false
                }
            } else {
                currentField.append(c)
            }
        } else {
            when (c) {
                '"' -> inQuotes = true
                delimiter -> {
                    currentRow.add(currentField.toString().trim())
                    currentField.setLength(0)
                }
                '\n', '\r' -> {
                    if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
                        currentRow.add(currentField.toString().trim())
                        rows.add(ArrayList(currentRow))
                        currentRow.clear()
                        currentField.setLength(0)
                    }
                    if ((c == '\r') && ((i + 1) < allText.length) && (allText[i + 1] == '\n')) {
                        i++
                    }
                }
                else -> currentField.append(c)
            }
        }
        i++
    }
    if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
        currentRow.add(currentField.toString().trim())
        rows.add(currentRow)
    }
    return rows
}

private fun csvValue(row: List<String>, index: Int): String? {
    return if ((index != -1) && (row.size > index)) row[index].ifBlank { null } else null
}
