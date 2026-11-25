package com.luminousworksllc.mylingual.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * Thin wrapper around Android's on-device [SpeechRecognizer].
 *
 * SpeechRecognizer must be created and driven from the main thread, and it calls back on the
 * main thread too. The ViewModel is the only caller, and it always runs on the main thread.
 *
 * Partial results are streamed while the user speaks. After [stop], the recognizer delivers
 * the final transcript through [Listener.onFinal], or an error through [Listener.onError].
 */
class SpeechTranscriber(
    private val context: Context,
    private val listener: Listener,
) {

    interface Listener {
        /** Live, unfinished transcript while the user is still speaking. */
        fun onPartial(text: String)

        /** Final transcript after the user stopped speaking. */
        fun onFinal(text: String)

        /** Human-readable error. Recording has already ended. */
        fun onError(message: String)

        /** Microphone loudness, normalised to 0..1, for the listening animation. */
        fun onLevel(level: Float)
    }

    private var recognizer: SpeechRecognizer? = null

    /** @param languageTag BCP-47 tag such as "en-US" or "es-ES". */
    fun start(languageTag: String) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            listener.onError("Speech recognition isn't available on this device.")
            return
        }
        val rec = recognizer ?: SpeechRecognizer.createSpeechRecognizer(context).also {
            it.setRecognitionListener(recognitionListener)
            recognizer = it
        }
        rec.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            }
        )
    }

    /** Ends recording now. The final transcript (or an error) arrives asynchronously. */
    fun stop() {
        recognizer?.stopListening()
    }

    fun destroy() {
        recognizer?.destroy()
        recognizer = null
    }

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) = Unit

        override fun onBeginningOfSpeech() = Unit

        override fun onRmsChanged(rmsdB: Float) {
            // Typical speech sits between about -2 and +10 dB.
            listener.onLevel(((rmsdB + 2f) / 12f).coerceIn(0f, 1f))
        }

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() {
            listener.onLevel(0f)
        }

        override fun onError(error: Int) {
            listener.onError(errorMessage(error))
        }

        override fun onResults(results: Bundle?) {
            val text = results.firstResult()
            if (text.isNullOrBlank()) {
                listener.onError(NOT_HEARD)
            } else {
                listener.onFinal(text)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            partialResults.firstResult()?.let { listener.onPartial(it) }
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private fun Bundle?.firstResult(): String? =
        this?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()

    private fun errorMessage(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_NO_MATCH,
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> NOT_HEARD

        SpeechRecognizer.ERROR_NETWORK,
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech recognition needs a network connection for this language."

        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "MyLingua needs microphone access to hear you."

        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "The speech recognizer is busy. Try again in a moment."

        else -> "Speech recognition failed (error $error)."
    }

    companion object {
        const val NOT_HEARD = "I didn't catch that. Try again."
    }
}
