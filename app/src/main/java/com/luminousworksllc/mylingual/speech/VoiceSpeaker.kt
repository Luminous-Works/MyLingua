package com.luminousworksllc.mylingual.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import java.util.Locale
import java.util.UUID

/**
 * Reads translations aloud with Android's [TextToSpeech] engine.
 *
 * Initialisation is asynchronous. A [speak] call made before the engine is ready is queued
 * and played as soon as it is ready.
 */
class VoiceSpeaker(context: Context) : TextToSpeech.OnInitListener {

    private class Queued(val text: String, val locale: Locale, val onDone: () -> Unit)

    private var ready = false
    private var queued: Queued? = null

    /** Called when the current utterance finishes or fails. Cleared by [stop]. */
    @Volatile
    private var onFinished: (() -> Unit)? = null

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext, this)

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            Log.e(TAG, "TextToSpeech failed to initialise (status=$status)")
            queued?.onDone?.invoke()
            queued = null
            return
        }
        ready = true
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) = finished()

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) = finished()

            override fun onError(utteranceId: String?, errorCode: Int) = finished()
        })
        queued?.let { speakNow(it.text, it.locale, it.onDone) }
        queued = null
    }

    /**
     * Speaks [text] in [locale], using the best available voice for that language.
     * [onDone] runs once speech has finished, or immediately if the language cannot be spoken.
     */
    fun speak(text: String, locale: Locale, onDone: () -> Unit) {
        if (!ready) {
            queued = Queued(text, locale, onDone)
            return
        }
        speakNow(text, locale, onDone)
    }

    /** Silences speech. [onDone] is not called for an utterance stopped this way. */
    fun stop() {
        queued = null
        onFinished = null
        tts.stop()
    }

    fun shutdown() {
        onFinished = null
        tts.shutdown()
    }

    private fun speakNow(text: String, locale: Locale, onDone: () -> Unit) {
        val languageResult = tts.setLanguage(locale)
        if (languageResult == TextToSpeech.LANG_MISSING_DATA ||
            languageResult == TextToSpeech.LANG_NOT_SUPPORTED
        ) {
            Log.w(TAG, "No TTS voice data for $locale (result=$languageResult)")
            onDone()
            return
        }

        pickVoice(locale)?.let { tts.voice = it }
        tts.setSpeechRate(SPEECH_RATE)
        tts.setPitch(1.0f)

        onFinished = onDone
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
    }

    /**
     * Prefers a voice for the exact country (es-ES over es-US), then the highest quality.
     * Voices that need the network come last, because the app should keep working offline.
     */
    private fun pickVoice(locale: Locale): Voice? =
        tts.voices.orEmpty()
            .filter { it.locale.language == locale.language }
            .sortedWith(
                compareByDescending<Voice> { it.locale.country == locale.country }
                    .thenByDescending { it.quality }
                    .thenBy { it.isNetworkConnectionRequired }
            )
            .firstOrNull()

    private fun finished() {
        val callback = onFinished
        onFinished = null
        callback?.invoke()
    }

    companion object {
        private const val TAG = "MyLingua"
        private const val SPEECH_RATE = 0.95f
    }
}
