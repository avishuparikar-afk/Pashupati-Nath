package com.pashuraksha.ai

import android.content.Context
import com.pashuraksha.BuildConfig
import com.pashuraksha.data.OfflineDataRepository
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * AI backend for the chatbot.
 *
 *   online  → Google Gemini 2.0 Flash (key from BuildConfig.GEMINI_API_KEY)
 *   offline → local keyword matcher over assets/data/chatbot.csv
 *
 * No Retrofit, no heavy deps — just HttpURLConnection. Keeps APK small and
 * the code easy to audit.
 *
 * The API key is NEVER hardcoded. It comes from local.properties via
 * BuildConfig so the key stays out of git.
 */
object GeminiClient {

    private const val GEMINI_ENDPOINT =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"

    private const val SYSTEM_PROMPT = """
You are Pashu Doctor AI, a friendly livestock health advisor for Indian rural
farmers. Answer in the language the farmer uses (Hindi, Marathi, or English).
Keep answers SHORT, PRACTICAL, and STRUCTURED:
  • Likely issue
  • 2-3 home care steps
  • When to call vet

Avoid jargon. If the farmer describes an emergency (sudden death, blood
discharge, severe bloat, breathing failure) tell them to CALL A VET NOW.
"""

    /**
     * Ask the AI. Blocks — call from a background coroutine / thread.
     * Returns a user-facing string (never throws to the caller).
     */
    fun ask(ctx: Context, userMessage: String, history: List<Pair<String, String>>): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        // If key missing or placeholder, fall straight back to offline mode
        if (apiKey.isBlank() || apiKey == "YOUR_GEMINI_API_KEY_HERE") {
            return offlineAnswer(ctx, userMessage)
        }

        return try {
            callGemini(apiKey, userMessage, history)
        } catch (t: Throwable) {
            // Network fail / API error → graceful offline fallback
            val fallback = offlineAnswer(ctx, userMessage)
            "$fallback\n\n(offline mode — check internet for AI answers)"
        }
    }

    // ---------------------------------------------------------------------
    // Online — Gemini 2.0 Flash via REST
    // ---------------------------------------------------------------------
    private fun callGemini(
        apiKey: String,
        userMessage: String,
        history: List<Pair<String, String>>
    ): String {
        val url = URL("$GEMINI_ENDPOINT?key=$apiKey")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 15000
            readTimeout = 30000
            setRequestProperty("Content-Type", "application/json")
        }

        // Build Gemini request payload
        val contents = JSONArray()

        // Replay last ~6 history turns for context
        history.takeLast(6).forEach { (role, text) ->
            val geminiRole = if (role == "user") "user" else "model"
            contents.put(
                JSONObject().apply {
                    put("role", geminiRole)
                    put("parts", JSONArray().put(JSONObject().put("text", text)))
                }
            )
        }

        // Current user turn (prepend system prompt once in front)
        contents.put(
            JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(
                    JSONObject().put("text", "$SYSTEM_PROMPT\n\nFarmer: $userMessage")
                ))
            }
        )

        val body = JSONObject().apply {
            put("contents", contents)
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.6)
                put("maxOutputTokens", 512)
            })
        }.toString()

        conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val response = stream.bufferedReader().use(BufferedReader::readText)

        if (code !in 200..299) {
            throw RuntimeException("Gemini HTTP $code: $response")
        }

        // Parse response
        val root = JSONObject(response)
        val candidates = root.optJSONArray("candidates") ?: return "…"
        if (candidates.length() == 0) return "…"
        val parts = candidates.getJSONObject(0)
            .optJSONObject("content")
            ?.optJSONArray("parts") ?: return "…"
        if (parts.length() == 0) return "…"
        return parts.getJSONObject(0).optString("text", "").trim()
            .ifEmpty { "I didn't catch that. Please ask again." }
    }

    // ---------------------------------------------------------------------
    // Offline — keyword matcher over assets/data/chatbot.csv
    // ---------------------------------------------------------------------
    private fun offlineAnswer(ctx: Context, userMessage: String): String {
        val msg = userMessage.lowercase()
        val rows = readChatbotCsv(ctx)

        // Score each row by how many keywords appear
        var bestRow: Array<String>? = null
        var bestScore = 0
        for (row in rows) {
            if (row.size < 3) continue
            val keywords = row[1].split("|").map { it.trim().lowercase() }
            val hits = keywords.count { it.isNotEmpty() && msg.contains(it) }
            if (hits > bestScore) {
                bestScore = hits
                bestRow = row
            }
        }

        if (bestRow != null && bestScore > 0) {
            return bestRow[2]
        }

        // No CSV match — try symptom-based offline diagnosis engine
        val hits = OfflineDataRepository.getSymptoms()
            .filter { msg.contains(it.key.replace("_", " ")) || msg.contains(it.labelEn.lowercase()) }
            .map { it.key }
            .toSet()

        if (hits.isNotEmpty()) {
            val results = OfflineDataRepository.diagnose(hits)
            if (results.isNotEmpty()) {
                val top = results.first()
                return buildString {
                    append("Possible: ${top.disease.name} (${top.confidence}% match)\n\n")
                    append("Home care:\n")
                    top.disease.homeCare.take(3).forEach { append("• $it\n") }
                    append("\nVet: ${top.disease.vetAdvice}")
                }
            }
        }

        return "I'm not sure. Describe the symptoms — fever, mouth sores, swelling, diarrhea, limping, etc. Or tap the 📷 Scan Animal button for image-based check."
    }

    private fun readChatbotCsv(ctx: Context): List<Array<String>> {
        return try {
            ctx.assets.open("data/chatbot.csv").use { input ->
                BufferedReader(InputStreamReader(input)).useLines { lines ->
                    lines.filter { it.isNotBlank() }
                        .drop(1)
                        .map { it.split(",").map(String::trim).toTypedArray() }
                        .toList()
                }
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }
}
