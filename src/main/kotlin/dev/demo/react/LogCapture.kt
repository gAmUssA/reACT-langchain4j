package dev.demo.react

import java.io.OutputStream
import java.io.PrintStream

/**
 * Captures stderr output (SLF4J SimpleLogger) into a ring buffer.
 * The original stderr is preserved so logs still go to the real stderr
 * when the TUI is not active.
 */
object LogCapture {

    private const val MAX_LINES = 500
    private val buffer = ArrayDeque<String>(MAX_LINES)
    private var installed = false

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
        synchronized(buffer) {
            if (buffer.size >= MAX_LINES) buffer.removeFirst()
            buffer.addLast(line)
        }
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
