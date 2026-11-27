package com.luminousworksllc.mylingual.data

import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.luminousworksllc.mylingual.model.Language
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/**
 * Translates text with Gemini through Firebase AI Logic.
 */
class GeminiTranslator {

    private val model: GenerativeModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig { temperature = 0.2f },
            systemInstruction = content { text(SYSTEM_INSTRUCTION) },
        )
    }

    suspend fun translate(text: String, from: Language, to: Language): String {
        val prompt = "Source language: ${from.promptName}. Target language: ${to.promptName}.\n\n$text"
        var attempts = 0
        var lastException: Exception? = null
        while (attempts < 3) {
            try {
                attempts++
                val reply = model.generateContent(prompt).text?.trim()
                if (!reply.isNullOrEmpty()) {
                    return reply
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastException = e
                if (attempts < 3) {
                    delay(500L * attempts)
                }
            }
        }
        throw lastException ?: IllegalStateException("Gemini returned an empty translation")
    }

    companion object {
        const val MODEL_NAME = "gemini-1.5-flash"

        const val SYSTEM_INSTRUCTION =
            "You are MiLingua, a highly skilled interpreter. Translate the input " +
                "between English and Spanish. Ensure the output is grammatically flawless, " +
                "natural-sounding, and culturally appropriate. Only return the translated text " +
                "itself, with no extra commentary."
    }
}
