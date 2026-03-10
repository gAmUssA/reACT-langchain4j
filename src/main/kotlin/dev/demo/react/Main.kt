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
    val apiKey = System.getenv("ANTHROPIC_API_KEY")
    if (apiKey.isNullOrBlank()) {
        System.err.println("ERROR: ANTHROPIC_API_KEY environment variable is not set.")
        System.err.println("Export it before running:  export ANTHROPIC_API_KEY=sk-ant-...")
        exitProcess(1)
    }

    val chatModel = AnthropicChatModel.builder()
        .apiKey(apiKey)
        .modelName("claude-sonnet-4-20250514")
        .logRequests(true)
        .logResponses(true)
        .build()

    val assistant = AiServices.builder(TravelAssistant::class.java)
        .chatModel(chatModel)
        .tools(WeatherTool(), CurrencyTool(), AttractionsTool())
        .chatMemory(MessageWindowChatMemory.withMaxMessages(20))
        .build()

    val app = TravelApp(assistant)
    Program(app).withAltScreen().run()
}
