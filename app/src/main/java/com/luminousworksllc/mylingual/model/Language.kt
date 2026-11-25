package com.luminousworksllc.mylingual.model

import java.util.Locale

/** The two languages MyLingua translates between. */
enum class Language(
    /** Label shown on the language pill. */
    val displayName: String,
    /** Name used in the Gemini prompt. */
    val promptName: String,
    /** BCP-47 tag passed to SpeechRecognizer as EXTRA_LANGUAGE. */
    val speechTag: String,
    /** Locale used to choose the TextToSpeech language and voice. */
    val locale: Locale,
) {
    ENGLISH("English", "English", "en-US", Locale.forLanguageTag("en-US")),
    SPANISH("Español", "Spanish", "es-ES", Locale.forLanguageTag("es-ES"));

    /** The language this one translates into. */
    val other: Language
        get() = if (this == ENGLISH) SPANISH else ENGLISH
}
