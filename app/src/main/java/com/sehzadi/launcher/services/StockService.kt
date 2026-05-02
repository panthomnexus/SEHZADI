package com.sehzadi.launcher.services

import com.sehzadi.launcher.ai.services.GeminiService
import com.sehzadi.launcher.ai.services.TavilyService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class StockData(
    val ticker: String,
    val price: Double,
    val change: Double,
    val changePercent: Double,
    val high: Double,
    val low: Double,
    val volume: Long,
    val aiAnalysis: String = ""
)

@Singleton
class StockService @Inject constructor(
    private val geminiService: GeminiService,
    private val tavilyService: TavilyService
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun analyzeTicker(ticker: String): String = withContext(Dispatchers.IO) {
        try {
            val searchResults = tavilyService.search("$ticker stock price today analysis", 3)
            val marketData = searchResults.joinToString("\n") {
                "${it.title}: ${it.content.take(200)}"
            }

            val prompt = """Analyze this stock: $ticker
                |Market data:
                |$marketData
                |
                |Provide:
                |1. Current price trend
                |2. Risk level (Low/Medium/High)
                |3. Brief summary (2-3 lines)
                |4. Recommendation
                |Respond in Hinglish.""".trimMargin()

            val analysis = geminiService.chat(prompt)
            analysis
        } catch (e: Exception) {
            "Stock analysis unavailable: ${e.message}"
        }
    }

    suspend fun getStockPrice(ticker: String): StockData? = withContext(Dispatchers.IO) {
        try {
            val results = tavilyService.search("$ticker stock price today", 1)
            val content = results.firstOrNull()?.content ?: return@withContext null

            StockData(
                ticker = ticker.uppercase(),
                price = 0.0,
                change = 0.0,
                changePercent = 0.0,
                high = 0.0,
                low = 0.0,
                volume = 0L,
                aiAnalysis = content.take(300)
            )
        } catch (e: Exception) {
            null
        }
    }
}
