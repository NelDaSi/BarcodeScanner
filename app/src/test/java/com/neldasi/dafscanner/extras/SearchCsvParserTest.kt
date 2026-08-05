package com.neldasi.dafscanner.extras

import org.junit.Assert.*
import org.junit.Test

class SearchCsvParserTest {

    // MX11 code reused from PartParserTest: typeCode 2150001, serialHex 939DE0, serialDecimal 9674208
    private val mx11Code = "215000188429939DE000000000K6805"

    @Test
    fun blankInput_returnsEmptyList() {
        assertTrue(parseSearchItemsCsv("").isEmpty())
        assertTrue(parseSearchItemsCsv("   \n  ").isEmpty())
    }

    @Test
    fun semicolonDelimited_parsesHeaderAndRow() {
        val csv = """
            Product ID;Machine;Output Material;Start Date;Start Time
            $mx11Code;M100;ModelX;2026-01-01;08:00
        """.trimIndent()

        val items = parseSearchItemsCsv(csv)

        assertEquals(1, items.size)
        val item = items[0]
        assertEquals("2150001", item.typeCode)
        assertEquals("939DE0", item.serialNumber)
        assertEquals("9674208", item.decSerial)
        assertEquals("M100", item.machine)
        assertEquals("ModelX", item.outputMaterial)
        assertEquals("2026-01-01", item.startDate)
        assertEquals("08:00", item.startTime)
    }

    @Test
    fun commaDelimited_parsesWhenNoSemicolonInHeader() {
        val csv = "Product ID,Machine\n$mx11Code,M200"

        val items = parseSearchItemsCsv(csv)

        assertEquals(1, items.size)
        assertEquals("M200", items[0].machine)
    }

    @Test
    fun duplicateSerials_keepsOnlyFirstOccurrence() {
        val csv = """
            Product ID;Machine
            $mx11Code;FIRST
            $mx11Code;SECOND
        """.trimIndent()

        val items = parseSearchItemsCsv(csv)

        assertEquals(1, items.size)
        assertEquals("FIRST", items[0].machine)
    }

    @Test
    fun quotedFields_preserveEmbeddedDelimiterAndEscapedQuotes() {
        val csv = "Product ID;Output Material\n$mx11Code;\"Model \"\"X\"\"; Special\""

        val items = parseSearchItemsCsv(csv)

        assertEquals(1, items.size)
        assertEquals("Model \"X\"; Special", items[0].outputMaterial)
    }

    @Test
    fun alternateHeaderAliases_areRecognized() {
        val csv = "Transport Number;Workstation;Model\n$mx11Code;M300;VariantY"

        val items = parseSearchItemsCsv(csv)

        assertEquals(1, items.size)
        assertEquals("M300", items[0].machine)
        assertEquals("VariantY", items[0].outputMaterial)
    }

    @Test
    fun blankOrShortProductId_isSkipped() {
        val csv = """
            Product ID;Machine
            ;M100
            TOOSHORT;M200
            $mx11Code;M300
        """.trimIndent()

        val items = parseSearchItemsCsv(csv)

        assertEquals(1, items.size)
        assertEquals("M300", items[0].machine)
    }

    @Test
    fun noRecognizableHeader_returnsEmptyList() {
        val csv = "Foo;Bar\n$mx11Code;M100"

        assertTrue(parseSearchItemsCsv(csv).isEmpty())
    }

    @Test
    fun crlfLineEndings_areHandled() {
        val csv = "Product ID;Machine\r\n$mx11Code;M100\r\n"

        val items = parseSearchItemsCsv(csv)

        assertEquals(1, items.size)
        assertEquals("M100", items[0].machine)
    }

    @Test
    fun missingOptionalColumns_areNullNotBlank() {
        val csv = "Product ID\n$mx11Code"

        val items = parseSearchItemsCsv(csv)

        assertEquals(1, items.size)
        assertNull(items[0].machine)
        assertNull(items[0].outputMaterial)
    }
}
