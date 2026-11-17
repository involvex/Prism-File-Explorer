package com.raival.compose.file.explorer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShellResultParserTest {

    @Test
    fun shellQuoteEscapesDangerousCharacters() {
        val path = "/sdcard/a'b; rm -rf / #.txt"
        assertEquals("'/sdcard/a'\\''b; rm -rf / #.txt'", path.shellQuote())

        val withDollar = "/sdcard/\$HOME `id`.txt"
        assertEquals("'/sdcard/\$HOME `id`.txt'", withDollar.shellQuote())
    }

    @Test
    fun parsesSuccessfulCommand() {
        val result = parseShellResult("0\n/file/a.txt\n/file/b.txt\n")
        assertEquals(0, result?.exitCode)
        assertTrue(result!!.isSuccess)
        assertEquals("/file/a.txt\n/file/b.txt\n", result.output)
    }

    @Test
    fun parsesSuccessfulCommandWithEmptyOutput() {
        val result = parseShellResult("0\n")
        assertTrue(result!!.isSuccess)
        assertEquals("", result.output)
    }

    @Test
    fun nonzeroExitCodeIsFailureEvenWithoutErrorText() {
        val result = parseShellResult("1\n")
        assertFalse(result!!.isSuccess)
        assertEquals(1, result.exitCode)
    }

    @Test
    fun serviceExceptionCodeIsFailure() {
        val result = parseShellResult("999\nShell execution failed")
        assertFalse(result!!.isSuccess)
        assertEquals("Shell execution failed", result.output)
    }

    @Test
    fun successIsNotInferredFromOutputText() {
        // Old callers treated any output that didn't contain "denied" as success.
        // With the wire format, exit code is authoritative.
        val withDeniedText = parseShellResult("0\nsome denied word in output")
        assertTrue(withDeniedText!!.isSuccess)

        val failedWithoutDenied = parseShellResult("2\nNo such file or directory")
        assertFalse(failedWithoutDenied!!.isSuccess)
    }

    @Test
    fun malformedAndMissingResultsAreNull() {
        assertNull(parseShellResult(null))
        assertNull(parseShellResult(""))
        assertNull(parseShellResult("no separator"))
        assertNull(parseShellResult("not-a-code\noutput"))
    }
}
