package dev.demo.react

import com.williamcallahan.tui4j.compat.bubbles.spinner.Spinner
import com.williamcallahan.tui4j.compat.bubbles.spinner.SpinnerType
import com.williamcallahan.tui4j.compat.bubbles.textinput.TextInput
import com.williamcallahan.tui4j.compat.bubbles.viewport.Viewport
import com.williamcallahan.tui4j.compat.bubbletea.*
import com.williamcallahan.tui4j.compat.bubbletea.message.WindowSizeMessage
import com.williamcallahan.tui4j.compat.lipgloss.Borders
import com.williamcallahan.tui4j.compat.lipgloss.Join
import com.williamcallahan.tui4j.compat.lipgloss.Position
import com.williamcallahan.tui4j.compat.lipgloss.Style
import com.williamcallahan.tui4j.compat.lipgloss.color.Color

// Custom message for agent response
data class AgentResponseMessage(val answer: String) : Message

class TravelApp(
    private val assistant: TravelAssistant,
    private val modelName: String = "",
    private val langchain4jVersion: String = "",
) : Model {

    private var terminalWidth = 80
    private var terminalHeight = 24

    private val input = TextInput().also { ti ->
        ti.setPlaceholder("Ask about a trip (e.g., Plan a 3-day trip to Tokyo on a \$1500 budget)")
        ti.setWidth(74)
        ti.focus()
    }

    private var spinner = Spinner(SpinnerType.DOT)
    private var chatViewport = Viewport.create(76, 16)
    private var logViewport = Viewport.create(76, 16)

    private val messages = mutableListOf<ChatEntry>()
    private var isLoading = false
    private var showLogs = false
    private var lastLogLineCount = 0

    // Styles
    private val titleStyle = Style.newStyle()
        .foreground(Color.color("99")).bold(true)
        .align(Position.Center)
    private val subtitleStyle = Style.newStyle()
        .foreground(Color.color("241"))
        .align(Position.Center)
    private val userStyle = Style.newStyle().foreground(Color.color("86"))
    private val toolStyle = Style.newStyle().foreground(Color.color("214"))
    private val assistantStyle = Style.newStyle().foreground(Color.color("255"))
    private val dimStyle = Style.newStyle().foreground(Color.color("241"))
    private val headingStyle = Style.newStyle().foreground(Color.color("212")).bold(true)
    private val h2Style = Style.newStyle().foreground(Color.color("117")).bold(true)
    private val h3Style = Style.newStyle().foreground(Color.color("150")).bold(true)
    private val boldStyle = Style.newStyle().bold(true)
    private val bulletStyle = Style.newStyle().foreground(Color.color("214"))
    private val blockquoteStyle = Style.newStyle().foreground(Color.color("245")).italic(true)
    private val codeStyle = Style.newStyle().foreground(Color.color("222"))
    private val hrColor = Color.color("240")
    private val logTitleStyle = Style.newStyle().foreground(Color.color("214")).bold(true).align(Position.Center)
    private val logDimStyle = Style.newStyle().foreground(Color.color("243"))

    override fun init(): Command {
        return Command.batch(
            TextInput::blink,
            spinner.init(),
            Command.checkWindowSize()
        )
    }

    /** The currently active viewport (chat or logs). */
    private val activeViewport: Viewport
        get() = if (showLogs) logViewport else chatViewport

    override fun update(msg: Message): UpdateResult<out Model> {
        when (msg) {
            is WindowSizeMessage -> {
                terminalWidth = msg.width()
                terminalHeight = msg.height()
                resizeComponents()
                rebuildViewportContent()
                refreshLogViewport()
                return UpdateResult.from(this)
            }

            is AgentResponseMessage -> {
                isLoading = false
                messages.add(ChatEntry(Role.ASSISTANT, msg.answer))
                input.focus()
                rebuildViewportContent()
                chatViewport.gotoBottom()
                return UpdateResult.from(this)
            }

            is KeyPressMessage -> {
                when (msg.key()) {
                    "ctrl+c" -> return UpdateResult.from(this, Command.quit())

                    // Toggle log panel with Ctrl+O
                    "ctrl+o" -> {
                        showLogs = !showLogs
                        if (showLogs) {
                            refreshLogViewport()
                            logViewport.gotoBottom()
                        }
                        return UpdateResult.from(this)
                    }

                    "enter" -> {
                        if (showLogs) return UpdateResult.from(this) // no input in log view
                        val query = input.value().trim()
                        if (query.isEmpty() || isLoading) return UpdateResult.from(this)

                        if (query.equals("q", ignoreCase = true) || query.equals("quit", ignoreCase = true)) {
                            return UpdateResult.from(this, Command.quit())
                        }

                        messages.add(ChatEntry(Role.USER, query))
                        input.setValue("")
                        isLoading = true
                        input.blur()
                        rebuildViewportContent()
                        chatViewport.gotoBottom()

                        val agentCommand = Command {
                            val answer = assistant.chat(query)
                            AgentResponseMessage(answer)
                        }

                        return UpdateResult.from(this, Command.batch(spinner.init(), agentCommand))
                    }
                }

                // Scroll keys — always work in log view, or in chat when not typing
                if (showLogs || isLoading || !input.isFocused) {
                    when (msg.key()) {
                        "up", "k" -> { activeViewport.scrollUp(1); return UpdateResult.from(this) }
                        "down", "j" -> { activeViewport.scrollDown(1); return UpdateResult.from(this) }
                        "pgup" -> { activeViewport.pageUp(); return UpdateResult.from(this) }
                        "pgdown" -> { activeViewport.pageDown(); return UpdateResult.from(this) }
                    }
                }
            }
        }

        // Update spinner while loading — also refresh log viewport to show new logs
        if (isLoading) {
            refreshLogViewportIfChanged()
            val spinnerResult = spinner.update(msg)
            spinner = spinnerResult.model()
            return UpdateResult.from(this, spinnerResult.command())
        }

        // Forward to active viewport for mouse wheel / built-in keys
        activeViewport.update(msg)
        // Update text input (only matters in chat view)
        if (!showLogs) input.update(msg)
        return UpdateResult.from(this)
    }

    override fun view(): String {
        val contentWidth = terminalWidth - 2

        // Title bar
        val titleText = if (showLogs) "Smart Trip Planner — Logs" else "Smart Trip Planner"
        val title = titleStyle.width(contentWidth).render(titleText)
        val bannerParts = mutableListOf("ReACT + LangChain4j Demo")
        if (modelName.isNotEmpty()) bannerParts.add(modelName)
        if (langchain4jVersion.isNotEmpty()) bannerParts.add("LC4j $langchain4jVersion")
        val subtitle = subtitleStyle.width(contentWidth).render(bannerParts.joinToString("  ·  "))
        val separator = dimStyle.render("─".repeat(contentWidth))
        val titleBlock = Join.joinVertical(Position.Left, title, subtitle, separator)

        // Active viewport
        val vpView = activeViewport.view()

        // Input area / log mode indicator
        val inputArea: String
        if (showLogs) {
            val logCount = LogCapture.lineCount()
            val logInfo = logDimStyle.render("  $logCount log lines captured")
            inputArea = Style.newStyle()
                .border(Borders.roundedBorder())
                .borderForeground(Color.color("214"))
                .width(contentWidth - 2)
                .padding(0, 1)
                .render(logInfo)
        } else if (isLoading) {
            val loadingText = "  ${spinner.view()} ${toolStyle.render("Thinking... (using tools to plan your trip)")}"
            inputArea = Style.newStyle()
                .border(Borders.roundedBorder())
                .borderForeground(Color.color("214"))
                .width(contentWidth - 2)
                .padding(0, 1)
                .render(loadingText)
        } else {
            inputArea = Style.newStyle()
                .border(Borders.roundedBorder())
                .borderForeground(Color.color("86"))
                .width(contentWidth - 2)
                .padding(0, 1)
                .render(input.view())
        }

        // Status bar
        val vp = activeViewport
        val scrollInfo = if (vp.totalLineCount() > vp.visibleLineCount()) {
            val pct = (vp.scrollPercent() * 100).toInt()
            " │ ${pct}%"
        } else ""
        val logToggle = if (showLogs) "ctrl+o chat" else "ctrl+o logs"
        val statusBar = dimStyle.render("  ↑↓ scroll │ $logToggle │ enter send │ ctrl+c quit$scrollInfo")

        return Join.joinVertical(Position.Left, titleBlock, vpView, inputArea, statusBar)
    }

    private fun resizeComponents() {
        val contentWidth = terminalWidth - 4
        val vpHeight = terminalHeight - 8
        input.setWidth(contentWidth - 4)
        chatViewport.setWidth(terminalWidth - 2)
        chatViewport.setHeight(vpHeight)
        logViewport.setWidth(terminalWidth - 2)
        logViewport.setHeight(vpHeight)
    }

    private fun rebuildViewportContent() {
        val contentWidth = terminalWidth - 6
        val parts = mutableListOf<String>()

        if (messages.isEmpty()) {
            val welcome = Style.newStyle()
                .foreground(Color.color("245"))
                .italic(true)
                .align(Position.Center)
                .width(contentWidth)
                .render("Type a question below to start planning your trip!")
            parts.add(welcome)
        }

        for (entry in messages) {
            when (entry.role) {
                Role.USER -> parts.add(renderUserMessage(entry.content, contentWidth))
                Role.ASSISTANT -> parts.add(renderAssistantMessage(entry.content, contentWidth))
            }
        }

        if (isLoading) {
            parts.add("")
        }

        chatViewport.setContent(parts.joinToString("\n\n"))
    }

    /** Refresh the log viewport content from the capture buffer. */
    private fun refreshLogViewport() {
        lastLogLineCount = LogCapture.lineCount()
        val content = LogCapture.content()
        if (content.isBlank()) {
            logViewport.setContent(logDimStyle.render("  No logs captured yet. Logs appear when the LLM is called."))
        } else {
            logViewport.setContent(content)
        }
    }

    /** Only refresh log viewport if new lines have been captured (avoids flicker). */
    private fun refreshLogViewportIfChanged() {
        val current = LogCapture.lineCount()
        if (current != lastLogLineCount) {
            refreshLogViewport()
            if (showLogs) logViewport.gotoBottom()
        }
    }

    private fun renderUserMessage(content: String, width: Int): String {
        val label = userStyle.bold(true).render(" You ")
        val body = Style.newStyle().foreground(Color.color("86")).render(content)
        val inner = "$label $body"
        return Style.newStyle()
            .border(Borders.roundedBorder())
            .borderForeground(Color.color("86"))
            .width(width)
            .padding(0, 1)
            .render(inner)
    }

    private fun renderAssistantMessage(content: String, width: Int): String {
        val label = assistantStyle.bold(true).render(" Assistant ")
        val innerWidth = width - 4
        val rendered = renderMarkdown(content, innerWidth)
        val inner = "$label\n$rendered"
        return Style.newStyle()
            .border(Borders.roundedBorder())
            .borderForeground(Color.color("255"))
            .width(width)
            .padding(0, 1)
            .render(inner)
    }

    private fun renderMarkdown(raw: String, contentWidth: Int): String {
        val lines = raw.lines()
        val result = StringBuilder()
        val boldRegex = Regex("\\*\\*(.+?)\\*\\*")
        val inlineCodeRegex = Regex("`([^`]+)`")

        for (line in lines) {
            val trimmed = line.trim()
            val rendered = when {
                // Horizontal rule
                trimmed.matches(Regex("^-{3,}$|^\\*{3,}$|^_{3,}$")) -> {
                    Style.newStyle().foreground(hrColor)
                        .render("─".repeat(contentWidth.coerceAtMost(60)))
                }
                // H1
                trimmed.startsWith("# ") -> {
                    val text = trimmed.removePrefix("# ").let { applyInlineStyles(it, boldRegex, inlineCodeRegex) }
                    headingStyle.render("━━ $text ━━")
                }
                // H2
                trimmed.startsWith("## ") -> {
                    val text = trimmed.removePrefix("## ").let { applyInlineStyles(it, boldRegex, inlineCodeRegex) }
                    h2Style.render("▸ $text")
                }
                // H3
                trimmed.startsWith("### ") -> {
                    val text = trimmed.removePrefix("### ").let { applyInlineStyles(it, boldRegex, inlineCodeRegex) }
                    h3Style.render("  ▹ $text")
                }
                // Bullet list
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    val text = trimmed.substring(2).let { applyInlineStyles(it, boldRegex, inlineCodeRegex) }
                    "  ${bulletStyle.render("●")} $text"
                }
                // Numbered list
                trimmed.matches(Regex("^\\d+\\.\\s.*")) -> {
                    val num = trimmed.substringBefore(".")
                    val text = trimmed.substringAfter(". ").let { applyInlineStyles(it, boldRegex, inlineCodeRegex) }
                    "  ${bulletStyle.render("$num.")} $text"
                }
                // Blockquote
                trimmed.startsWith("> ") -> {
                    val text = trimmed.removePrefix("> ").let { applyInlineStyles(it, boldRegex, inlineCodeRegex) }
                    "  ${Style.newStyle().foreground(Color.color("240")).render("│")} ${blockquoteStyle.render(text)}"
                }
                // Empty line
                trimmed.isEmpty() -> ""
                // Regular text with inline styling
                else -> applyInlineStyles(trimmed, boldRegex, inlineCodeRegex)
            }
            result.appendLine(rendered)
        }
        return result.toString().trimEnd()
    }

    private fun applyInlineStyles(text: String, boldRegex: Regex, codeRegex: Regex): String {
        var result = text
        // Apply bold **text**
        result = boldRegex.replace(result) { match ->
            boldStyle.render(match.groupValues[1])
        }
        // Apply inline code `text`
        result = codeRegex.replace(result) { match ->
            codeStyle.render(match.groupValues[1])
        }
        return result
    }

    private data class ChatEntry(val role: Role, val content: String)
    private enum class Role { USER, ASSISTANT }
}
