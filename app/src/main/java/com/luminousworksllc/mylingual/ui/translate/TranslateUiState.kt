package com.luminousworksllc.mylingual.ui.translate

import com.luminousworksllc.mylingual.model.Language

/** What the app is doing right now. Exactly one phase at a time. */
sealed interface Phase {
    /** Waiting for the microphone to be pressed. */
    data object Idle : Phase

    /** Recording. [partial] is the live, unfinished transcript. */
    data class Listening(val partial: String = "") : Phase

    /** Gemini is translating the final transcript. */
    data object Translating : Phase

    /** Text-to-speech is reading the translation aloud. */
    data object Speaking : Phase
}

data class TranslateUiState(
    val phase: Phase = Phase.Idle,
    /** Language the user speaks. The other language is the output. */
    val source: Language = Language.ENGLISH,
    /** Final transcript of the latest utterance. */
    val transcript: String = "",
    /** Translation of [transcript]. */
    val translation: String = "",
    /** True after a quick tap: keep listening until the next tap. */
    val handsFree: Boolean = false,
    /** One-line message for errors, such as "I didn't catch that." */
    val message: String? = null,
)
