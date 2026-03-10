package dev.demo.react

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import java.io.OutputStream
import java.io.PrintStream

/**
 * Captures stderr output (SLF4J SimpleLogger) into a ring buffer.
 * JSON fragments embedded in log lines are detected and pretty-printed.
 */
object LogCapture {

    private const val MAX_LINES = 2000
    private val buffer = ArrayDeque<String>(MAX_LINES)
    private var installed = false

    private val mapper = ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)

    /** Install a stderr redirect that captures lines into the ring buffer. */
    fun install() {
        if (installed) return
        installed = true
        val original = System.err
        val capturing = PrintStream(CapturingOutputStream(original), true)
        System.setErr(capturing)
    }

    /** Return all captured log lines joined as a single string. */
    fun content(): String = synchronized(buffer) {
        buffer.joinToString("\n")
    }

    /** Number of captured lines. */
    fun lineCount(): Int = synchronized(buffer) { buffer.size }

    private fun addLine(line: String) {
        val formatted = formatLine(line)
        synchronized(buffer) {
            for (l in formatted.lines()) {
                if (buffer.size >= MAX_LINES) buffer.removeFirst()
                buffer.addLast(l)
            }
        }
    }

    /**
     * If the line contains an embedded JSON object or array, split it into
     * a prefix + pretty-printed JSON. Otherwise return as-is.
     */
    private fun formatLine(line: String): String {
        // Find the first '{' or '[' that might start a JSON blob
        val jsonStart = findJsonStart(line) ?: return line

        val prefix = line.substring(0, jsonStart).trimEnd()
        val jsonCandidate = line.substring(jsonStart)

        return try {
            val tree = mapper.readTree(jsonCandidate)
            val pretty = mapper.writeValueAsString(tree)
            if (prefix.isNotEmpty()) "$prefix\n$pretty" else pretty
        } catch (_: JsonProcessingException) {
            line
        }
    }

    /**
     * Find the index of the first `{` or `[` that could be the start of a
     * JSON body in a log line. Skips common log prefixes like timestamps,
     * log levels, and logger names.
     */
    private fun findJsonStart(line: String): Int? {
        val braceIdx = line.indexOf('{')
        val bracketIdx = line.indexOf('[')
        // Pick whichever comes first, if any
        val candidates = listOfNotNull(
            if (braceIdx >= 0) braceIdx else null,
            if (bracketIdx >= 0) bracketIdx else null
        )
        if (candidates.isEmpty()) return null
        val idx = candidates.min()
        // Only treat as JSON if it's after some prefix (not the very start of a log-level word)
        // and if there's a matching closer somewhere
        val closer = if (line[idx] == '{') '}' else ']'
        return if (line.lastIndexOf(closer) > idx) idx else null
    }

    /**
     * OutputStream that splits incoming bytes into lines, stores each in the
     * ring buffer, and suppresses output to the original stream (since TUI4J
     * owns the terminal).
     */
    private class CapturingOutputStream(private val original: OutputStream) : OutputStream() {
        private val lineBuilder = StringBuilder()

        override fun write(b: Int) {
            val c = b.toChar()
            if (c == '\n') {
                addLine(lineBuilder.toString())
                lineBuilder.clear()
            } else {
                lineBuilder.append(c)
            }
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            for (i in off until off + len) {
                write(b[i].toInt())
            }
        }

        override fun flush() {
            // Don't flush to original — TUI owns the terminal
        }
    }
}
