package com.example

import com.example.ui.components.ExcelReportExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvExportSecurityTest {

    @Test
    fun `escapeCsv handles plain text without modifications`() {
        val input = "Wyciskanie sztangi leżąc"
        val result = ExcelReportExporter.escapeCsv(input)
        assertEquals("Wyciskanie sztangi leżąc", result)
    }

    @Test
    fun `escapeCsv escapes commas, semicolons, quotes and newlines according to RFC 4180`() {
        // Semicolon (CSV column separator in European format)
        assertEquals("\"Seria 1; uwaga na łokcie\"", ExcelReportExporter.escapeCsv("Seria 1; uwaga na łokcie"))

        // Comma
        assertEquals("\"Tempo 3,1,1,0\"", ExcelReportExporter.escapeCsv("Tempo 3,1,1,0"))

        // Quotes - must be doubled
        assertEquals("\"Powtórzenia \"\"hardcore\"\"\"", ExcelReportExporter.escapeCsv("Powtórzenia \"hardcore\""))

        // Newline
        assertEquals("\"Linia 1\nLinia 2\"", ExcelReportExporter.escapeCsv("Linia 1\nLinia 2"))
    }

    @Test
    fun `escapeCsv neutralizes formulas starting with equals, plus, minus, at`() {
        // Equal sign
        val formulaEquals = "=SUM(A1:A10)"
        val escapedEquals = ExcelReportExporter.escapeCsv(formulaEquals)
        assertTrue("Musi być w cudzysłowach", escapedEquals.startsWith("\"") && escapedEquals.endsWith("\""))
        assertTrue("Musi być poprzedzone apostrofem chroniącym przed formułą", escapedEquals.contains("'=SUM(A1:A10)"))

        // At sign
        val formulaAt = "@SUM(B1:B5)"
        val escapedAt = ExcelReportExporter.escapeCsv(formulaAt)
        assertTrue(escapedAt.contains("'@SUM(B1:B5)"))

        // Plus with text (non-numeric formula)
        val formulaPlus = "+cmd|' /C calc'!A0"
        val escapedPlus = ExcelReportExporter.escapeCsv(formulaPlus)
        assertTrue(escapedPlus.contains("'+cmd|' /C calc'!A0"))

        // Minus with text
        val formulaMinus = "-2+3+cmd|' /C calc'!A0"
        val escapedMinus = ExcelReportExporter.escapeCsv(formulaMinus)
        assertTrue(escapedMinus.contains("'-2+3+cmd|' /C calc'!A0"))
    }

    @Test
    fun `escapeCsv neutralizes formulas preceded by whitespace or control characters`() {
        // Leading spaces before formula
        val spacedFormula = "   =1+1"
        val escapedSpaced = ExcelReportExporter.escapeCsv(spacedFormula)
        assertTrue("Musi neutralizować formułę poprzedzoną spacjami", escapedSpaced.contains("'   =1+1"))

        // Leading tab and newline
        val tabbedFormula = "\t\r\n=HYPERLINK(\"http://evil.com\")"
        val escapedTabbed = ExcelReportExporter.escapeCsv(tabbedFormula)
        assertTrue("Musi neutralizować formułę poprzedzoną znakami kontrolnymi", escapedTabbed.contains("'\t\r\n=HYPERLINK"))
    }

    @Test
    fun `escapeCsv preserves Polish characters and UTF-8 diacritics without distortion`() {
        val polishText = "Zażółć gęślą jaźń: Klatka piersiowa, przysiady, łydki, ścięgna, mięśnie brzucha"
        val result = ExcelReportExporter.escapeCsv(polishText)
        assertTrue("Polskie znaki diakrytyczne muszą pozostać nienaruszone", result.contains("Zażółć gęślą jaźń"))
        assertTrue(result.contains("Klatka piersiowa"))
        assertTrue(result.contains("mięśnie brzucha"))
    }

    @Test
    fun `escapeCsv preserves valid numeric values without converting them into formulas`() {
        // Direct Numbers
        assertEquals("100", ExcelReportExporter.escapeCsv(100))
        assertEquals("20.5", ExcelReportExporter.escapeCsv(20.5))
        assertEquals("-5.0", ExcelReportExporter.escapeCsv(-5.0))

        // String numbers starting with + or -
        assertEquals("20.5", ExcelReportExporter.escapeCsv("20.5"))
        assertEquals("-5.0", ExcelReportExporter.escapeCsv("-5.0"))
        assertEquals("+12.5", ExcelReportExporter.escapeCsv("+12.5"))

        // Valid negative weight or deficit
        assertFalse("Prawidłowa liczba ujemna nie może być traktowana jako wstrzyknięcie formuły", ExcelReportExporter.escapeCsv("-2.5").contains("'-2.5"))
    }

    @Test
    fun `escapeCsv handles null and empty values cleanly`() {
        assertEquals("", ExcelReportExporter.escapeCsv(null))
        assertEquals("", ExcelReportExporter.escapeCsv(""))
    }
}
