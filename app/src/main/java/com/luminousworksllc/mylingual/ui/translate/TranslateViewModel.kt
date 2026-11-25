package com.luminousworksllc.mylingual.ui.translate

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.luminousworksllc.mylingual.data.GeminiTranslator
import com.luminousworksllc.mylingual.model.Language
import com.luminousworksllc.mylingual.speech.SpeechTranscriber
import com.luminousworksllc.mylingual.speech.VoiceSpeaker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Drives the whole pipeline:
 *
 *   press -> Listening (SpeechRecognizer) -> release -> Translating (Gemini)
 *         -> Speaking (TextToSpeech) -> Idle
 *
 * Press handling supports two gestures:
 *  - Press and hold: listen while the finger is down, translate on release.
 *  - Quick tap (under [TAP_MAX_MILLIS]): listen hands-free until the next tap.
 *
 * All state lives in [state]. The UI only reads it and sends press events.
 */
class TranslateViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(TranslateUiState())
    val state: StateFlow<TranslateUiState> = _state.asStateFlow()

    /** Microphone loudness 0..1, used only by the listening animation. */
    private val _soundLevel = MutableStateFlow(0f)
    val soundLevel: StateFlow<Float> = _soundLevel.asStateFlow()

    /** True when the press currently being held is the one that started listening. */
    private var pressStartedListening = false

    private val translator = GeminiTranslator()
    private val speaker = VoiceSpeaker(application)
    private val transcriber = SpeechTranscriber(application, object : SpeechTranscriber.Listener {
        override fun onPartial(text: String) {
            _state.update { if (it.phase is Phase.Listening) it.copy(phase = Phase.Listening(text)) else it }
        }

        override fun onFinal(text: String) = handleFinalTranscript(text)

        override fun onError(message: String) = fail(message)

        override fun onLevel(level: Float) {
            _soundLevel.value = level
        }
    })

    /** Switches the spoken language. Ignored while busy, so the indicator never lies. */
    fun setSource(language: Language) {
        if (_state.value.phase != Phase.Idle) return
        _state.update { it.copy(source = language, message = null) }
    }

    /** Finger went down on the microphone button. */
    fun onPressStart() {
        val current = _state.value
        pressStartedListening = false
        when {
            // A quick tap was latched on. This tap ends the recording.
            current.phase is Phase.Listening && current.handsFree -> {
                _state.update { it.copy(handsFree = false) }
                transcriber.stop()
            }
            current.phase == Phase.Idle || current.phase == Phase.Speaking -> startListening()
            else -> Unit // Translating: ignore taps until the translation is back.
        }
    }

    /**
     * Finger lifted from the microphone button.
     * @param heldMillis how long the finger was down.
     */
    fun onPressEnd(heldMillis: Long) {
        if (!pressStartedListening) return
        pressStartedListening = false
        if (_state.value.phase !is Phase.Listening) return

        if (heldMillis < TAP_MAX_MILLIS) {
            // Quick tap: keep listening hands-free until the next tap.
            _state.update { it.copy(handsFree = true) }
        } else {
            // Press and hold: release means "I'm done talking".
            transcriber.stop()
        }
    }

    private fun startListening() {
        speaker.stop()
        _soundLevel.value = 0f
        val source = _state.value.source
        _state.value = TranslateUiState(phase = Phase.Listening(), source = source)
        pressStartedListening = true
        transcriber.start(source.speechTag)
    }

    private fun handleFinalTranscript(text: String) {
        val heard = text.trim()
        if (heard.isEmpty()) {
            fail(SpeechTranscriber.NOT_HEARD)
        } else {
            translate(heard)
        }
    }

    private fun translate(original: String) {
        val from = _state.value.source
        val to = from.other
        _soundLevel.value = 0f
        _state.update {
            it.copy(
                phase = Phase.Translating,
                transcript = original,
                translation = "",
                handsFree = false,
                message = null,
            )
        }

        viewModelScope.launch {
            try {
                val translation = translator.translate(original, from, to)
                _state.update { it.copy(phase = Phase.Speaking, translation = translation) }
                speaker.speak(translation, to.locale) { finishSpeaking() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Translation failed", e)
                fail("Couldn't translate that. Check your connection and try again.")
            }
        }
    }

    private fun finishSpeaking() {
        // Only go idle if nothing newer has started since. Stale callbacks are ignored.
        _state.update { if (it.phase == Phase.Speaking) it.copy(phase = Phase.Idle) else it }
    }

    /** Returns to Idle and shows [message] to the user. */
    private fun fail(message: String) {
        pressStartedListening = false
        _soundLevel.value = 0f
        _state.update { it.copy(phase = Phase.Idle, handsFree = false, message = message) }
    }

    override fun onCleared() {
        transcriber.destroy()
        speaker.shutdown()
        super.onCleared()
    }

    companion object {
        private const val TAG = "MyLingua"

        /** Presses shorter than this are treated as a tap that latches listening on. */
        private const val TAP_MAX_MILLIS = 250L
    }
}
