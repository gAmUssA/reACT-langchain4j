package dev.demo.react

import com.williamcallahan.tui4j.compat.bubbletea.Program
import dev.langchain4j.memory.chat.MessageWindowChatMemory
import dev.langchain4j.model.anthropic.AnthropicChatModel
import dev.langchain4j.service.AiServices
import dev.demo.react.tools.AttractionsTool
import dev.demo.react.tools.CurrencyTool
import dev.demo.react.tools.WeatherTool
import kotlin.system.exitProcess

fun main() {
    // Capture stderr (SLF4J logs) before any logging happens
    LogCapture.install()

    val apiKey = System.getenv("ANTHROPIC_API_KEY")
    if (apiKey.isNullOrBlank()) {
        System.err.println("ERROR: ANTHROPIC_API_KEY environment variable is not set.")
        System.err.println("Export it before running:  export ANTHROPIC_API_KEY=sk-ant-...")
        exitProcess(1)
    }

    val modelName = "claude-sonnet-4-20250514"
    val langchain4jVersion = Thread.currentThread().contextClassLoader
        ?.getResourceAsStream("META-INF/maven/dev.langchain4j/langchain4j/pom.properties")
        ?.use { java.util.Properties().apply { load(it) }.getProperty("version") }
        ?: "unknown"

    val chatModel = AnthropicChatModel.builder()
        .apiKey(apiKey)
        .modelName(modelName)
        .logRequests(true)
        .logResponses(true)
        .build()

    val assistant = AiServices.builder(TravelAssistant::class.java)
        .chatModel(chatModel)
        .tools(WeatherTool(), CurrencyTool(), AttractionsTool())
        .chatMemory(MessageWindowChatMemory.withMaxMessages(20))
        .build()

    val app = TravelApp(assistant, modelName = modelName, langchain4jVersion = langchain4jVersion)
    Program(app).withAltScreen().run()
}
