// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: ApplicationLogger.kt provides a Centralized application logger providing consistent log APIs with sanitization,
 * timestamp, thread information, and safe handling of Android log limitations.
 * Designed for module-level usage to allow independent logging control.
 */
package com.infineon.secora.wallet.logger

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.os.Process
import android.provider.MediaStore
import android.util.Log
import com.infineon.secora.wallet.BuildConfig
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.Calendar
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * Enhanced ApplicationLogger with sanitization, timestamp, thread info, and log levels.
 */
class ApplicationLogger private constructor(private val tag: String) {

    companion object {
        private const val MAX_LOG_SIZE = 1000
        private const val MAX_TAG_LENGTH = 23
        private val MONTHS = arrayOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "July", "Aug", "Sep", "Oct", "Nov", "Dec"
        )
        private const val REDACTED_PLACEHOLDER = "$1: [REDACTED]"
        private const val LOG_RETENTION_MS = 10 * 60 * 1000L
        private const val LOG_DIR = "logs"
        private const val LOG_FILE_NAME = "recent_10min.log"
        private val PUBLIC_LOG_RELATIVE_PATH = Environment.DIRECTORY_DOWNLOADS + "/SecoraLogs"
        private const val PUBLIC_LOG_FILE_NAME = "secora_runtime_all.log.txt"
        private const val PUBLIC_LOG_MAX_BYTES = 5 * 1024 * 1024L
        private const val MIME_TYPE_TEXT_PLAIN = "text/plain"
        @Volatile
        private var appContext: Context? = null
        @Volatile
        private var publicLogUri: Uri? = null
        @Volatile
        private var logcatProcess: java.lang.Process? = null
        private val logcatCaptureStarted = AtomicBoolean(false)

        /**
         * Returns an [ApplicationLogger] instance for the given [tag],
         * truncating the tag to [MAX_TAG_LENGTH] when necessary.
         */
        fun getApplicationLogger(tag: String): ApplicationLogger {
            val safeTag = if (tag.length > MAX_TAG_LENGTH) tag.takeLast(MAX_TAG_LENGTH) else tag
            return ApplicationLogger(safeTag)
        }

        /**
         * Stores the application [context] and, when [BuildConfig.ENABLE_APP_LOGS]
         * is true, prepares the public log file and starts logcat capture.
         */
        fun initialize(context: Context) {
            appContext = context.applicationContext
            if (BuildConfig.ENABLE_APP_LOGS) {
                publicLogUri = try {
                    resolveOrCreatePublicLogUri(context.applicationContext)
                } catch (_: Exception) {
                    null
                }
                startLogcatCapture()
            }
        }

        /**
         * Returns the absolute path of the private recent-log file, or null
         * if [initialize] has not been called.
         */
        fun getLogFilePath(): String? {
            val context = appContext ?: return null
            return File(File(context.filesDir, LOG_DIR), LOG_FILE_NAME).absolutePath
        }

        /**
         * Returns the expected public Downloads path for the runtime log file.
         */
        fun getPublicLogPath(): String {
            return "/storage/emulated/0/Download/SecoraLogs/$PUBLIC_LOG_FILE_NAME"
        }

        /**
         * Exports recent retained logs to Downloads when [BuildConfig.ENABLE_APP_LOGS]
         * is true, using the context stored by [initialize].
         */
        fun exportRecentLogsToDownloads(): Uri? {
            if (!BuildConfig.ENABLE_APP_LOGS) return null
            val context = appContext ?: return null
            return exportRecentLogsToDownloads(context)
        }

        /**
         * Exports recent retained logs from the private log file to Downloads
         * when [BuildConfig.ENABLE_APP_LOGS] is true; returns null otherwise.
         */
        fun exportRecentLogsToDownloads(context: Context): Uri? {
            if (!BuildConfig.ENABLE_APP_LOGS) return null
            return try {
                val src = File(File(context.filesDir, LOG_DIR), LOG_FILE_NAME)
                if (!src.exists()) return null

                val nowMs = System.currentTimeMillis()
                val threshold = nowMs - LOG_RETENTION_MS
                val readableLines = src.readLines().mapNotNull { entry ->
                    val delimiter = entry.indexOf('|')
                    if (delimiter <= 0) return@mapNotNull null
                    val ts = entry.substring(0, delimiter).toLongOrNull() ?: return@mapNotNull null
                    if (ts < threshold) return@mapNotNull null
                    entry.substring(delimiter + 1)
                }

                val fileName = "secora_recent_10min_${nowMs}.log"
                val values = android.content.ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, MIME_TYPE_TEXT_PLAIN)
                    put(
                        MediaStore.Downloads.RELATIVE_PATH,
                        Environment.DIRECTORY_DOWNLOADS + "/SecoraLogs"
                    )
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
                resolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                    writer.write(readableLines.joinToString(separator = "\n", postfix = "\n"))
                }
                uri
            } catch (_: Exception) {
                null
            }
        }

        /**
         * Resolves an existing public runtime log MediaStore URI, or creates one
         * under Downloads/SecoraLogs when none is found.
         */
        private fun resolveOrCreatePublicLogUri(context: Context): Uri? {
            return try {
                val resolver = context.contentResolver
                // Match by display name only, then filter path: RELATIVE_PATH format varies by OS version
                // (with/without trailing slash), so strict equality often misses an existing row and
                // insert then fails with "Failed to build unique file".
                val projection = arrayOf(MediaStore.Downloads._ID, MediaStore.Downloads.RELATIVE_PATH)
                val existingId = resolver.query(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    projection,
                    "${MediaStore.Downloads.DISPLAY_NAME}=?",
                    arrayOf(PUBLIC_LOG_FILE_NAME),
                    null
                )?.use { cursor: Cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
                    val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.RELATIVE_PATH)
                    var id: Long? = null
                    while (cursor.moveToNext()) {
                        val path = cursor.getString(pathCol).orEmpty()
                        if (path.contains("SecoraLogs", ignoreCase = true)) {
                            id = cursor.getLong(idCol)
                            break
                        }
                    }
                    id
                }
                if (existingId != null) {
                    Uri.withAppendedPath(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        existingId.toString()
                    )
                } else {
                    val values = android.content.ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, PUBLIC_LOG_FILE_NAME)
                        put(MediaStore.Downloads.MIME_TYPE, MIME_TYPE_TEXT_PLAIN)
                        put(MediaStore.Downloads.RELATIVE_PATH, PUBLIC_LOG_RELATIVE_PATH)
                    }
                    try {
                        resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    } catch (_: IllegalStateException) {
                        insertUniquePublicRuntimeLog(resolver)
                    }
                }
            } catch (_: Exception) {
                null
            }
        }

        /**
         * Inserts a uniquely named public runtime log when the default filename
         * already exists in MediaStore but was not returned by our query.
         */
        private fun insertUniquePublicRuntimeLog(resolver: android.content.ContentResolver): Uri? {
            return try {
                val values = android.content.ContentValues().apply {
                    put(
                        MediaStore.Downloads.DISPLAY_NAME,
                        "secora_runtime_all_${System.currentTimeMillis()}.txt"
                    )
                    put(MediaStore.Downloads.MIME_TYPE, MIME_TYPE_TEXT_PLAIN)
                    put(MediaStore.Downloads.RELATIVE_PATH, PUBLIC_LOG_RELATIVE_PATH)
                }
                resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            } catch (_: Exception) {
                null
            }
        }

        /**
         * Starts a background thread that captures this process's logcat output
         * and appends each line to the public runtime log file.
         */
        private fun startLogcatCapture() {
            if (!logcatCaptureStarted.compareAndSet(false, true)) return
            val context = appContext ?: return
            thread(name = "secora-logcat-capture", isDaemon = true) {
                try {
                    val pid = Process.myPid()
                    val processBuilder = java.lang.ProcessBuilder(
                        "logcat",
                        "--pid=$pid",
                        "-v",
                        "time"
                    )
                    processBuilder.redirectErrorStream(true)
                    val process = processBuilder.start()
                    logcatProcess = process
                    BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                        while (true) {
                            val line = reader.readLine() ?: break
                            appendRawPublicLogLine(context, line)
                        }
                    }
                } catch (_: Exception) {
                    // Best-effort capture only.
                }
            }
        }

        /**
         * Appends a raw logcat [entry] to the public runtime log, resolving the
         * MediaStore URI on demand when it is not yet cached.
         */
        private fun appendRawPublicLogLine(context: Context, entry: String) {
            try {
                val uri = publicLogUri ?: resolveOrCreatePublicLogUri(context)?.also { publicLogUri = it } ?: return
                writePublicLogEntryWithCap(context, uri, entry)
            } catch (_: Exception) {
                // Best-effort write only.
            }
        }

        /**
         * Writes [entry] to the public log at [uri], truncating the file when
         * [PUBLIC_LOG_MAX_BYTES] is exceeded.
         */
        private fun writePublicLogEntryWithCap(context: Context, uri: Uri, entry: String) {
            val currentSize = context.contentResolver.query(
                uri,
                arrayOf(MediaStore.Downloads.SIZE),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Downloads.SIZE))
                } else {
                    0L
                }
            } ?: 0L

            val mode = if (currentSize >= PUBLIC_LOG_MAX_BYTES) "wt" else "wa"
            context.contentResolver.openOutputStream(uri, mode)?.bufferedWriter()?.use { writer ->
                writer.write(entry)
                writer.newLine()
            }
        }
    }


    /** Log levels */
    enum class Level { ERROR, WARN, DEBUG, INFO, TRACE }

    /**
     * Returns a human-readable timestamp string for the current time.
     */
    private fun now(): String {
        val cal = Calendar.getInstance()
        val mon = MONTHS[cal[Calendar.MONTH]]
        val day = cal[Calendar.DAY_OF_MONTH]
        val year = cal[Calendar.YEAR]
        val hr = cal[Calendar.HOUR_OF_DAY]
        val min = cal[Calendar.MINUTE]
        val sec = cal[Calendar.SECOND]
        return "$mon-$day-$year $hr:$min:$sec"
    }

    /**
     * Redacts sensitive values such as passwords, tokens, keys, and card numbers
     * from the given log [msg].
     */
    private fun sanitize(msg: String): String {
        return msg
            .replace(Regex("(?i)(password|pwd|pass)\\s*[:=]\\s*[^\\s]+"), REDACTED_PLACEHOLDER)
            .replace(Regex("(?i)(token|jwt|auth)\\s*[:=]\\s*[^\\s]+"), REDACTED_PLACEHOLDER)
            .replace(Regex("(?i)(key|secret)\\s*[:=]\\s*[^\\s]+"), REDACTED_PLACEHOLDER)
            .replace(Regex("\\b\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}\\b"), "[CARD_NUMBER_REDACTED]")
    }

    /**
     * Emits a sanitized [message] at the given [level] when logging is enabled,
     * splitting long messages and optionally persisting them.
     */
    private fun log(level: Level, message: String) {
        if (!shouldEmitLog()) return

        val sanitized = sanitize(message)
        val nowMs = System.currentTimeMillis()
        val logMsg = "[${now()}] [${Thread.currentThread().name}] [$level] $tag : $sanitized"

        // Split long logs
        var start = 0
        while (start < logMsg.length) {
            val end = (start + MAX_LOG_SIZE).coerceAtMost(logMsg.length)
            val chunk = logMsg.substring(start, end)
            when (level) {
                Level.ERROR -> Log.e(tag, chunk)
                Level.WARN -> Log.w(tag, chunk)
                Level.DEBUG -> Log.d(tag, chunk)
                Level.INFO -> Log.i(tag, chunk)
                Level.TRACE -> Log.v(tag, chunk)
            }
            if (BuildConfig.ENABLE_APP_LOGS) {
                persistLogLine(nowMs, chunk)
            }
            start = end
        }
    }

    /**
     * Returns true when [BuildConfig.ENABLE_APP_LOGS] is enabled.
     */
    private fun shouldEmitLog(): Boolean = BuildConfig.ENABLE_APP_LOGS

    /**
     * Appends a timestamped [line] to the private recent-log file and the public
     * runtime log when [BuildConfig.ENABLE_APP_LOGS] is true.
     */
    private fun persistLogLine(timestampMs: Long, line: String) {
        if (!BuildConfig.ENABLE_APP_LOGS) return
        try {
            val context = appContext ?: return
            val dir = File(context.filesDir, LOG_DIR)
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, LOG_FILE_NAME)
            // Avoid expensive full-file read per log line; append only.
            file.appendText("$timestampMs|$line\n")
            appendToPublicRuntimeLog("$timestampMs|$line")
        } catch (_: Exception) {
            // Logging persistence must never crash app flow.
        }
    }

    /**
     * Appends [entry] to the public runtime log when [BuildConfig.ENABLE_APP_LOGS]
     * is true, resolving the MediaStore URI on demand if needed.
     */
    private fun appendToPublicRuntimeLog(entry: String) {
        if (!BuildConfig.ENABLE_APP_LOGS) return
        try {
            val context = appContext ?: return
            val uri = publicLogUri ?: resolveOrCreatePublicLogUri(context)?.also { publicLogUri = it } ?: return
            writePublicLogEntryWithCap(context, uri, entry)
        } catch (_: Exception) {
            // Fallback to private logs only.
        }
    }

    /**
     * Logs an error-level [message] when logging is enabled.
     */
    fun error(message: String) = log(Level.ERROR, message)

    /**
     * Logs a debug-level [message] when [BuildConfig.ENABLE_APP_LOGS] is true.
     */
    fun debug(message: String) {
        if (BuildConfig.ENABLE_APP_LOGS) log(Level.DEBUG, message)
    }

    /**
     * Logs an info-level [message] when [BuildConfig.ENABLE_APP_LOGS] is true.
     */
    fun info(message: String) {
        if (BuildConfig.ENABLE_APP_LOGS) log(Level.INFO, message)
    }

    /**
     * Logs an error message with full exception detail when [BuildConfig.ENABLE_APP_LOGS]
     * is true, otherwise logs only the exception class name.
     */
    fun noStackTraceLog(message: String, throwable: Throwable) {
        if (BuildConfig.ENABLE_APP_LOGS) {
            log(Level.DEBUG, "$message | ${throwable.message}")
        } else {
            log(Level.DEBUG, "$message | Exception: ${throwable.javaClass.simpleName}")
        }
    }
}
