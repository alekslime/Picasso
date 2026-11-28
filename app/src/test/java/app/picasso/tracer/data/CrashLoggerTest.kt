package app.picasso.tracer.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CrashLoggerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `formatReport includes metadata and stack trace`() {
        val report = CrashLogger.formatReport(
            threadName = "main",
            throwable = IllegalStateException("boom"),
            timestamp = "2026-11-28 11:00:00 UTC",
            appVersion = "1.0.0 (1)",
            deviceInfo = "Pixel 9 (Android 16, API 36)",
        )
        assertTrue(report.contains("Picasso Crash Report"))
        assertTrue(report.contains("Thread: main"))
        assertTrue(report.contains("IllegalStateException: boom"))
    }

    @Test
    fun `record read and clear lifecycle works on file`() {
        val file = tempFolder.newFile("last_crash.txt").apply { delete() }
        val logger = CrashLogger(file)

        assertFalse(logger.hasReport())
        assertNull(logger.readReport())

        logger.record("worker", RuntimeException("oops"))
        assertTrue(logger.hasReport())
        val content = logger.readReport()!!
        assertTrue(content.contains("RuntimeException: oops"))
        assertTrue(content.contains("Thread: worker"))

        logger.clear()
        assertFalse(logger.hasReport())
        assertNull(logger.readReport())
        assertEquals(false, file.exists())
    }
}
