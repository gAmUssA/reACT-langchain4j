package dev.demo.react.tools

import dev.langchain4j.agent.tool.P
import dev.langchain4j.agent.tool.Tool

class CurrencyTool {

    // Rates relative to USD
    private val ratesToUsd = mapOf(
        "USD" to 1.0,
        "EUR" to 0.92,
        "GBP" to 0.79,
        "JPY" to 149.0,
        "THB" to 35.5,
        "MXN" to 17.2
    )

    @Tool("Convert an amount from one currency to another")
    fun convertCurrency(
        @P("Amount to convert") amount: Double,
        @P("Source currency code, e.g. USD, EUR, GBP, JPY, THB, MXN") fromCurrency: String,
        @P("Target currency code, e.g. USD, EUR, GBP, JPY, THB, MXN") toCurrency: String
    ): String {
        val from = fromCurrency.uppercase()
        val to = toCurrency.uppercase()

        val fromRate = ratesToUsd[from]
            ?: return "Unknown currency: $from. Supported: ${ratesToUsd.keys.joinToString(", ")}"
        val toRate = ratesToUsd[to]
            ?: return "Unknown currency: $to. Supported: ${ratesToUsd.keys.joinToString(", ")}"

        val amountInUsd = amount / fromRate
        val converted = amountInUsd * toRate
        return "%.2f %s = %.2f %s".format(amount, from, converted, to)
    }
}
