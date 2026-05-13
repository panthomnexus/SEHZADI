package com.sehzadi.launcher.ai.models

class RuleEngine {

    private val rules = listOf(
        Rule("greeting", listOf("hello", "hi", "namaste", "hey", "namaskar")) {
            "Namaste! Main SEHZADI hoon — aapka AI assistant. Kya karu aapke liye?"
        },
        Rule("thanks", listOf("thank", "shukriya", "dhanyavaad", "thanks")) {
            "Shukriya! Kuch aur help chahiye toh batao."
        },
        Rule("how_are_you", listOf("kaise ho", "how are you", "kaisa hai")) {
            "Main theek hoon! Aapki kya help karun?"
        },
        Rule("who_are_you", listOf("kaun ho", "who are you", "kya hai tu", "apna naam")) {
            "Main SEHZADI hoon — ek futuristic AI assistant jo aapke phone ko smart banata hai."
        },
        Rule("time", listOf("time kya", "kitne baje", "what time", "samay")) {
            val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
            "Abhi ${sdf.format(java.util.Date())} baj rahe hain."
        },
        Rule("date", listOf("date kya", "aaj kya", "what date", "tarikh")) {
            val sdf = java.text.SimpleDateFormat("dd MMMM yyyy, EEEE", java.util.Locale("hi", "IN"))
            "Aaj ${sdf.format(java.util.Date())} hai."
        },
        Rule("battery", listOf("battery", "charge")) {
            "Battery status dekhne ke liye HUD screen pe jao — wahan real-time stats hain."
        },
        Rule("weather", listOf("weather", "mausam")) {
            "Weather ke liye internet chahiye. API key configure karo Settings mein."
        },
        Rule("joke", listOf("joke", "mazak", "funny", "chutkula")) {
            listOf(
                "Ek programmer ne apni girlfriend ko bola: 'Tum mere life mein bug ho.' Girlfriend: 'Toh fix karo na!' Programmer: 'Nahi, tum feature ho.'",
                "WiFi ka password kya hai? Answer: thoda_patience_rakho",
                "Phone ne battery ko bola: 'Main tere bina adhura hoon.' Battery: 'Aur main tere bina charge nahi hota.'",
                "AI se pucha: 'Tumhe neend aati hai?' AI bola: 'Nahi, lekin mera server zaroor so jaata hai.'"
            ).random()
        },
        Rule("help", listOf("help", "madad", "kya kar sakta", "kya kar sakti", "features")) {
            "Main ye sab kar sakti hoon:\n" +
            "• App kholna: 'open WhatsApp'\n" +
            "• Call karna: 'call Rahul'\n" +
            "• Message bhejhna: 'message Amit ko'\n" +
            "• Photo lena: 'photo le lo'\n" +
            "• Clock widget: 'live clock'\n" +
            "• AI chat: kuch bhi poocho\n" +
            "• Stock analysis: 'Tesla ka analysis'\n" +
            "• Web search: 'search karo'\n" +
            "Aur bahut kuch! Bas bolo."
        },
        Rule("chat", listOf("baat", "baat kar", "ai se baat", "talk")) {
            "Haan bolo! Main sun rahi hoon. Kuch bhi poocho — main jawab dungi."
        },
        Rule("ready", listOf("sab set", "ready", "all set", "done")) {
            "Sahi hai! Ab bolo kya karna hai — main ready hoon har kaam ke liye."
        },
        Rule("good_morning", listOf("good morning", "subah", "suprabhat")) {
            "Suprabhat! Aaj ka din acha ho. Kya karu aapke liye?"
        },
        Rule("good_night", listOf("good night", "shubh ratri", "raat")) {
            "Shubh ratri! Phone rakh do aur aaram karo. Kal milte hain!"
        },
        Rule("music", listOf("music", "gana", "song")) {
            "Music sunne ke liye 'open Spotify' ya 'open YouTube Music' bolo."
        },
        Rule("reminder", listOf("remind", "yaad dila")) {
            "Reminders feature coming soon! Tab tak notes save karo — 'note likh' bolo."
        },
        Rule("calculate", listOf("kitna hota", "calculate", "hisab")) {
            "Calculator ke liye 'open Calculator' bolo. Complex math ke liye AI model load karo."
        }
    )

    fun process(input: String): String? {
        val lower = input.lowercase()
        for (rule in rules) {
            if (rule.triggers.any { lower.contains(it) }) {
                return rule.respond()
            }
        }
        return null
    }
}

private data class Rule(
    val id: String,
    val triggers: List<String>,
    val respond: () -> String
)
