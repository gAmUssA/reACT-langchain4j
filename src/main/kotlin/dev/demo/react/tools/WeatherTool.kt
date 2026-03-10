package dev.demo.react.tools

import dev.langchain4j.agent.tool.P
import dev.langchain4j.agent.tool.Tool

class WeatherTool {

    private val weatherData = mapOf(
        "tokyo" to WeatherInfo(22, "Partly cloudy", listOf("Day 1: 23°C, sunny", "Day 2: 20°C, light rain", "Day 3: 22°C, clear skies")),
        "paris" to WeatherInfo(18, "Sunny", listOf("Day 1: 19°C, sunny", "Day 2: 17°C, overcast", "Day 3: 20°C, clear")),
        "london" to WeatherInfo(14, "Overcast", listOf("Day 1: 13°C, rainy", "Day 2: 15°C, cloudy", "Day 3: 14°C, drizzle")),
        "new york" to WeatherInfo(25, "Clear skies", listOf("Day 1: 26°C, sunny", "Day 2: 24°C, humid", "Day 3: 27°C, clear")),
        "bangkok" to WeatherInfo(33, "Hot and humid", listOf("Day 1: 34°C, sunny", "Day 2: 32°C, thunderstorm", "Day 3: 33°C, partly cloudy")),
        "mexico city" to WeatherInfo(21, "Mild and dry", listOf("Day 1: 22°C, sunny", "Day 2: 20°C, clear", "Day 3: 21°C, partly cloudy"))
    )

    @Tool("Get current weather and 3-day forecast for a city")
    fun getWeather(@P("City name to check weather for") city: String): String {
        val info = weatherData[city.lowercase()]
            ?: return "Weather data not available for '$city'. Available cities: ${weatherData.keys.joinToString(", ")}."

        return """
            Current weather in $city: ${info.tempC}°C, ${info.conditions}.
            3-day forecast:
            ${info.forecast.joinToString("\n")}
        """.trimIndent()
    }

    private data class WeatherInfo(val tempC: Int, val conditions: String, val forecast: List<String>)
}
