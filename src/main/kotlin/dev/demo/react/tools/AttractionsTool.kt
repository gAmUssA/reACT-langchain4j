package dev.demo.react.tools

import dev.langchain4j.agent.tool.P
import dev.langchain4j.agent.tool.Tool

class AttractionsTool {

    private val attractions = mapOf(
        "tokyo" to listOf(
            "Senso-ji Temple - Tokyo's oldest temple in Asakusa with iconic red gate",
            "Shibuya Crossing - World's busiest pedestrian intersection",
            "Tsukiji Outer Market - Fresh sushi and street food paradise",
            "Meiji Shrine - Serene Shinto shrine surrounded by forest",
            "Akihabara - Electric town for anime, manga, and electronics"
        ),
        "paris" to listOf(
            "Eiffel Tower - Iconic iron lattice tower with city views",
            "Louvre Museum - World's largest art museum, home of the Mona Lisa",
            "Montmartre & Sacre-Coeur - Bohemian hilltop neighborhood",
            "Notre-Dame Cathedral - Gothic masterpiece (under restoration)",
            "Seine River Cruise - Scenic boat ride past major landmarks"
        ),
        "london" to listOf(
            "British Museum - World-class collection of art and antiquities",
            "Tower of London - Historic castle and Crown Jewels",
            "Buckingham Palace - Official residence of the monarch",
            "Camden Market - Eclectic food, fashion, and music scene",
            "West End Theatre District - World-famous live performances"
        ),
        "new york" to listOf(
            "Central Park - 843-acre urban oasis in Manhattan",
            "Statue of Liberty - Iconic symbol of freedom on Liberty Island",
            "Metropolitan Museum of Art - One of the world's great art museums",
            "Times Square - Neon-lit entertainment hub",
            "Brooklyn Bridge - Historic suspension bridge with stunning views"
        ),
        "bangkok" to listOf(
            "Grand Palace & Wat Phra Kaew - Ornate royal complex with Emerald Buddha",
            "Chatuchak Weekend Market - Over 15,000 stalls of everything imaginable",
            "Wat Arun - Temple of Dawn on the Chao Phraya River",
            "Khao San Road - Backpacker hub with vibrant nightlife",
            "Floating Markets - Traditional canal-side markets outside the city"
        ),
        "mexico city" to listOf(
            "Zocalo & Templo Mayor - Main square with Aztec ruins",
            "Chapultepec Castle - Hilltop castle with murals and city views",
            "Frida Kahlo Museum (Casa Azul) - Iconic artist's colorful home",
            "Coyoacan - Charming colonial neighborhood with cafes and markets",
            "Teotihuacan Pyramids - Ancient city with massive pyramids (day trip)"
        )
    )

    @Tool("Search for top tourist attractions and activities in a city")
    fun searchAttractions(@P("City name to search attractions for") city: String): String {
        val cityAttractions = attractions[city.lowercase()]
            ?: return "No attraction data for '$city'. Available cities: ${attractions.keys.joinToString(", ")}."

        return "Top attractions in $city:\n" + cityAttractions.mapIndexed { i, a -> "${i + 1}. $a" }.joinToString("\n")
    }
}
