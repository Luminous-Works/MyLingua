package com.luminousworksllc.mylingual.data

import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.luminousworksllc.mylingual.model.Language

/**
 * Translates text with Gemini through Firebase AI Logic (the Gemini Developer API backend,
 * so no billing account is needed for the hackathon's free tier).
 *
 * The system instruction is fixed, so the model acts as an interpreter, not a word-for-word
 * translator. Only the user turn changes, and it carries the direction the user selected.
 */
class GeminiTranslator {

    private val model: GenerativeModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = MODEL_NAME,
            // Low temperature keeps grammar stable. Natural phrasing comes from the instruction.
            generationConfig = generationConfig { temperature = 0.2f },
            systemInstruction = content { text(SYSTEM_INSTRUCTION) },
        )
    }

    /**
     * @return the translated text only. Throws if the call fails (no network, App Check
     * rejected, API not enabled) or Gemini returns nothing.
     */
    suspend fun translate(text: String, from: Language, to: Language): String {
        val prompt = "Source language: ${from.promptName}. Target language: ${to.promptName}.\n\n$text"
        val reply = model.generateContent(prompt).text?.trim()
        if (reply.isNullOrEmpty()) {
            throw IllegalStateException("Gemini returned an empty translation")
        }
        return reply
    }

    companion object {
        /** Flash is fast enough for real-time conversation. Swap to a Pro model if quality matters more. */
        const val MODEL_NAME = "gemini-2.5-flash"

        const val SYSTEM_INSTRUCTION =
            "You are MyLingua, a highly skilled interpreter. Translate the following input " +
                "between English and Spanish. Ensure the output is grammatically flawless, " +
                "natural-sounding, and culturally appropriate. Only return the translated text " +
                "itself, with no extra commentary."
    }
}
