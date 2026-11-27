package com.luminousworksllc.mylingual

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

// ============================================================================
// 1. CARIBBEAN AQUATIC COLOR PALETTE
// ============================================================================

object CaribbeanPalette {
    val NightOcean = Color(0xFF050810)      // Dark Caribbean night ocean canvas
    val JuicyAqua = Color(0xFF00CEC9)       // Turquoise Caribbean ocean aqua
    val DeepLagoon = Color(0xFF0AB3A8)      // Deep lagoon cyan/teal
    val JuicyGold = Color(0xFFD4A843)       // Metallic Caribbean gold
    val DriftwoodWhite = Color(0xFFF8EDD0)  // Warm coastal driftwood off-white
    val LuminousGreen = Color(0xFF2ECC71)   // Active status green
    val DeepSpaceCard = Color(0xFF0D1322)   // Glassmorphic translucent card fill
    val CoralRed = Color(0xFFC0392B)        // Alert/Error red
}

// ============================================================================
// 2. MAIN ACTIVITY
// ============================================================================

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MiLinguaTheme {
                MiLinguaScreen()
            }
        }
    }
}

@Composable
fun MiLinguaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            background = CaribbeanPalette.NightOcean,
            surface = CaribbeanPalette.DeepSpaceCard,
            primary = CaribbeanPalette.JuicyAqua,
            onPrimary = CaribbeanPalette.NightOcean,
            secondary = CaribbeanPalette.JuicyGold,
            onSecondary = CaribbeanPalette.NightOcean,
            onBackground = CaribbeanPalette.DriftwoodWhite,
            onSurface = CaribbeanPalette.DriftwoodWhite,
            surfaceVariant = Color(0xFF141C30),
            onSurfaceVariant = Color(0xFFB0C4DE)
        ),
        content = content
    )
}

// ============================================================================
// 3. DATA MODELS & STATE
// ============================================================================

enum class TranslationDirection(
    val sourceLanguage: String,
    val targetLanguage: String,
    val sourceSpeechTag: String,
    val targetLocale: Locale
) {
    ENGLISH_TO_SPANISH("English", "Español", "en-US", Locale.forLanguageTag("es-ES")),
    SPANISH_TO_ENGLISH("Español", "English", "es-ES", Locale.forLanguageTag("en-US"));

    val other: TranslationDirection
        get() = if (this == ENGLISH_TO_SPANISH) SPANISH_TO_ENGLISH else ENGLISH_TO_SPANISH
}

sealed interface AppPhase {
    data object Idle : AppPhase
    data class Listening(val partialTranscript: String = "") : AppPhase
    data object Translating : AppPhase
    data object Speaking : AppPhase
}

data class MiLinguaUiState(
    val direction: TranslationDirection = TranslationDirection.ENGLISH_TO_SPANISH,
    val phase: AppPhase = AppPhase.Idle,
    val transcriptText: String = "",
    val translationText: String = "",
    val errorMessage: String? = null,
    val isHandsFree: Boolean = false
)

// ============================================================================
// 4. VIEWMODEL (MiLinguaViewModel)
// ============================================================================

class MiLinguaViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MiLinguaUiState())
    val uiState: StateFlow<MiLinguaUiState> = _uiState.asStateFlow()

    private val _soundLevel = MutableStateFlow(0f)
    val soundLevel: StateFlow<Float> = _soundLevel.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false
    private var pressStartedListening = false

    // Initialize Gemini model via Firebase AI Logic SDK
    private val generativeModel: GenerativeModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                temperature = 0.2f
            },
            systemInstruction = content {
                text(SYSTEM_INSTRUCTION)
            }
        )
    }

    init {
        textToSpeech = TextToSpeech(application.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = Unit
                    override fun onDone(utteranceId: String?) {
                        finishSpeaking()
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        finishSpeaking()
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        finishSpeaking()
                    }
                })
            } else {
                Log.e(TAG, "TextToSpeech init failed with status $status")
            }
        }
    }

    fun setDirection(direction: TranslationDirection) {
        if (_uiState.value.phase != AppPhase.Idle) return
        _uiState.update { it.copy(direction = direction, errorMessage = null) }
    }

    fun toggleDirection() {
        if (_uiState.value.phase != AppPhase.Idle) return
        _uiState.update { it.copy(direction = it.direction.other, errorMessage = null) }
    }

    fun onPressStart() {
        val current = _uiState.value
        pressStartedListening = false
        when {
            current.phase is AppPhase.Listening && current.isHandsFree -> {
                _uiState.update { it.copy(isHandsFree = false) }
                stopListeningInternal()
            }
            current.phase == AppPhase.Idle || current.phase == AppPhase.Speaking -> {
                startListeningInternal()
            }
            else -> Unit
        }
    }

    fun onPressEnd(heldMillis: Long) {
        if (!pressStartedListening) return
        pressStartedListening = false
        if (_uiState.value.phase !is AppPhase.Listening) return

        if (heldMillis < TAP_MAX_MILLIS) {
            _uiState.update { it.copy(isHandsFree = true) }
        } else {
            stopListeningInternal()
        }
    }

    private fun startListeningInternal() {
        stopSpeech()
        _soundLevel.value = 0f
        val currentDirection = _uiState.value.direction

        if (!SpeechRecognizer.isRecognitionAvailable(getApplication())) {
            fail("Speech recognition is not available on this device.")
            return
        }

        _uiState.update {
            MiLinguaUiState(
                direction = currentDirection,
                phase = AppPhase.Listening()
            )
        }
        pressStartedListening = true

        val context = getApplication<Application>().applicationContext
        val rec = speechRecognizer ?: SpeechRecognizer.createSpeechRecognizer(context).also {
            it.setRecognitionListener(recognitionListener)
            speechRecognizer = it
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentDirection.sourceSpeechTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        rec.startListening(intent)
    }

    private fun stopListeningInternal() {
        speechRecognizer?.stopListening()
    }

    private fun translateWithFirebaseAI(spokenText: String) {
        val direction = _uiState.value.direction
        _soundLevel.value = 0f
        _uiState.update {
            it.copy(
                phase = AppPhase.Translating,
                transcriptText = spokenText,
                translationText = "",
                isHandsFree = false,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            val prompt = "Source language: ${direction.sourceLanguage}. Target language: ${direction.targetLanguage}.\n\n$spokenText"
            var translatedText: String? = null

            // Firebase AI Logic is the only route to the model, so its own error is what
            // reaches the screen. Retries cover transient socket/network errors.
            var lastError: Exception? = null
            var attempts = 0
            while (attempts < 3 && translatedText.isNullOrEmpty()) {
                try {
                    attempts++
                    val response = generativeModel.generateContent(prompt)
                    translatedText = response.text?.trim()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    lastError = e
                    Log.w(TAG, "Firebase AI Logic call attempt $attempts failed: ${e.message}", e)
                    if (attempts < 3) {
                        kotlinx.coroutines.delay(500L * attempts)
                    }
                }
            }

            if (translatedText.isNullOrEmpty()) {
                fail(
                    lastError?.let { "Translation error: ${it.localizedMessage}" }
                        ?: "Gemini returned an empty translation."
                )
                return@launch
            }

            _uiState.update {
                it.copy(
                    phase = AppPhase.Speaking,
                    translationText = translatedText
                )
            }

            speakOutLoud(translatedText, direction.targetLocale)
        }
    }

    private fun speakOutLoud(text: String, locale: Locale) {
        if (!isTtsReady || textToSpeech == null) {
            finishSpeaking()
            return
        }

        val tts = textToSpeech!!
        val langResult = tts.setLanguage(locale)
        if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w(TAG, "Locale $locale not supported for TTS")
            finishSpeaking()
            return
        }

        tts.setSpeechRate(0.95f)
        tts.setPitch(1.0f)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
    }

    private fun stopSpeech() {
        textToSpeech?.stop()
    }

    private fun finishSpeaking() {
        _uiState.update { if (it.phase == AppPhase.Speaking) it.copy(phase = AppPhase.Idle) else it }
    }

    private fun fail(message: String) {
        pressStartedListening = false
        _soundLevel.value = 0f
        Log.e(TAG, "==========================================================")
        Log.e(TAG, "MILINGUA ERROR: $message")
        Log.e(TAG, "==========================================================")
        _uiState.update { it.copy(phase = AppPhase.Idle, isHandsFree = false, errorMessage = message) }
    }

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) = Unit
        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) {
            _soundLevel.value = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
        }
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() {
            _soundLevel.value = 0f
        }

        override fun onError(error: Int) {
            val errorMsg = when (error) {
                SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Didn't catch speech. Tap mic to try again."
                SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network connection required."
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
                else -> "Speech recognition error ($error)."
            }
            fail(errorMsg)
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull()?.trim()
            if (text.isNullOrEmpty()) {
                fail("Didn't catch that. Please speak again.")
            } else {
                translateWithFirebaseAI(text)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            matches?.firstOrNull()?.let { partial ->
                _uiState.update { state ->
                    if (state.phase is AppPhase.Listening) state.copy(phase = AppPhase.Listening(partial)) else state
                }
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    override fun onCleared() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        textToSpeech?.shutdown()
        textToSpeech = null
        super.onCleared()
    }

    companion object {
        private const val TAG = "MiLinguaViewModel"
        private const val TAP_MAX_MILLIS = 250L

        private const val SYSTEM_INSTRUCTION =
            "You are MiLingua, a highly skilled interpreter. Translate the input between " +
                "English and Spanish. Ensure the output is grammatically flawless, " +
                "natural-sounding, and culturally appropriate. Only return the translated text itself."
    }
}

// ============================================================================
// 5. JETPACK COMPOSE AQUATIC UI
// ============================================================================

@Composable
fun MiLinguaScreen(viewModel: MiLinguaViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val soundLevel by viewModel.soundLevel.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var micGranted by remember { mutableStateOf(context.checkAudioPermission()) }
    var askedForMic by rememberSaveable { mutableStateOf(false) }

    val micLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        micGranted = granted
        askedForMic = true
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        micGranted = context.checkAudioPermission()
    }

    val micBlocked = !micGranted && askedForMic &&
        context.findActivity()?.shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) == false

    MiLinguaContent(
        state = state,
        soundLevel = soundLevel,
        micGranted = micGranted,
        micBlocked = micBlocked,
        onDirectionChange = viewModel::setDirection,
        onDirectionToggle = viewModel::toggleDirection,
        onPressStart = viewModel::onPressStart,
        onPressEnd = viewModel::onPressEnd,
        onRequestMicPermission = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) },
        onOpenAppSettings = { context.openSettings() }
    )
}

@Composable
fun MiLinguaContent(
    state: MiLinguaUiState,
    soundLevel: Float,
    micGranted: Boolean,
    micBlocked: Boolean,
    onDirectionChange: (TranslationDirection) -> Unit,
    onDirectionToggle: () -> Unit,
    onPressStart: () -> Unit,
    onPressEnd: (Long) -> Unit,
    onRequestMicPermission: () -> Unit,
    onOpenAppSettings: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        CaribbeanPalette.NightOcean,
                        Color(0xFF081224),
                        CaribbeanPalette.NightOcean
                    )
                )
            )
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            AppHeaderBar()

            Spacer(modifier = Modifier.height(12.dp))

            // Caribbean Gold/Aqua Language Switcher
            AquaticLanguageSelectorBar(
                direction = state.direction,
                enabled = state.phase == AppPhase.Idle,
                onDirectionChange = onDirectionChange,
                onToggle = onDirectionToggle
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Live Transcript & Translation Display Area
            TranscriptAndTranslationCards(
                state = state,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Shimmering Water Ripple Microphone Section
            AquaticShimmeringMicSection(
                phase = state.phase,
                isHandsFree = state.isHandsFree,
                errorMessage = state.errorMessage,
                soundLevel = soundLevel,
                micGranted = micGranted,
                micBlocked = micBlocked,
                onPressStart = onPressStart,
                onPressEnd = onPressEnd,
                onRequestMicPermission = onRequestMicPermission,
                onOpenAppSettings = onOpenAppSettings
            )
        }
    }
}

// ----------------------------------------------------------------------------
// App Header Bar
// ----------------------------------------------------------------------------

@Composable
private fun AppHeaderBar() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "MILINGUA",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp,
                color = CaribbeanPalette.DriftwoodWhite,
                fontFamily = FontFamily.SansSerif
            )
            Spacer(modifier = Modifier.widthIn(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(CaribbeanPalette.JuicyGold)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "AI",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = CaribbeanPalette.NightOcean
                )
            }
        }
        Text(
            text = "SOVEREIGN CARIBBEAN VOICE INTERPRETER",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            color = CaribbeanPalette.JuicyAqua.copy(alpha = 0.85f),
            fontSize = 9.sp
        )
    }
}

// ----------------------------------------------------------------------------
// Language Selector Component
// ----------------------------------------------------------------------------

@Composable
private fun AquaticLanguageSelectorBar(
    direction: TranslationDirection,
    enabled: Boolean,
    onDirectionChange: (TranslationDirection) -> Unit,
    onToggle: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = CaribbeanPalette.DeepSpaceCard.copy(alpha = 0.8f),
        border = BorderStroke(1.dp, CaribbeanPalette.DeepLagoon.copy(alpha = 0.4f)),
        shadowElevation = 6.dp,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            LanguagePillButton(
                text = direction.sourceLanguage,
                selected = true,
                enabled = enabled,
                onClick = { }
            )

            // Gold Interchange Toggle Button
            Surface(
                onClick = onToggle,
                enabled = enabled,
                shape = CircleShape,
                color = CaribbeanPalette.JuicyGold,
                shadowElevation = 4.dp,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "⇄",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = CaribbeanPalette.NightOcean
                    )
                }
            }

            LanguagePillButton(
                text = direction.targetLanguage,
                selected = false,
                enabled = enabled,
                onClick = { onDirectionChange(direction.other) }
            )
        }
    }
}

@Composable
private fun LanguagePillButton(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(50),
        color = if (selected) CaribbeanPalette.JuicyAqua else Color.Transparent,
        contentColor = if (selected) CaribbeanPalette.NightOcean else CaribbeanPalette.DriftwoodWhite,
        border = if (selected) null else BorderStroke(1.dp, CaribbeanPalette.DeepLagoon.copy(alpha = 0.35f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Black else FontWeight.Medium,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp)
        )
    }
}

// ----------------------------------------------------------------------------
// Transcript and Translation Display Area
// ----------------------------------------------------------------------------

@Composable
private fun TranscriptAndTranslationCards(
    state: MiLinguaUiState,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val isListening = state.phase is AppPhase.Listening
    val currentSpoken = (state.phase as? AppPhase.Listening)?.partialTranscript
        ?: state.transcriptText

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (currentSpoken.isBlank() && state.translationText.isBlank() && state.phase == AppPhase.Idle) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = CaribbeanPalette.DeepSpaceCard.copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.dp, CaribbeanPalette.DeepLagoon.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🌊 READY FOR TRANSLATION",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = CaribbeanPalette.JuicyGold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Tap or hold the aquatic microphone button below to speak.",
                        style = MaterialTheme.typography.titleMedium,
                        color = CaribbeanPalette.DriftwoodWhite.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            if (currentSpoken.isNotBlank() || isListening) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = CaribbeanPalette.DeepSpaceCard.copy(alpha = 0.7f)
                    ),
                    border = BorderStroke(1.dp, CaribbeanPalette.DeepLagoon.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isListening) CaribbeanPalette.LuminousGreen else CaribbeanPalette.JuicyGold)
                            )
                            Spacer(modifier = Modifier.widthIn(8.dp))
                            Text(
                                text = "SPOKEN (${state.direction.sourceLanguage.uppercase()})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                color = CaribbeanPalette.JuicyGold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = currentSpoken.ifBlank { "Listening..." },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Medium,
                            color = if (isListening) CaribbeanPalette.JuicyAqua else CaribbeanPalette.DriftwoodWhite,
                            fontSize = 22.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            AnimatedVisibility(
                visible = state.translationText.isNotBlank() || state.phase == AppPhase.Translating,
                enter = fadeIn(tween(400)),
                exit = fadeOut(tween(200))
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF092032).copy(alpha = 0.85f)
                    ),
                    border = BorderStroke(1.5.dp, CaribbeanPalette.JuicyAqua.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(22.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "✨ MILINGUA TRANSLATION (${state.direction.targetLanguage.uppercase()})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                color = CaribbeanPalette.JuicyAqua
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (state.phase == AppPhase.Translating) "Translating with Gemini AI..." else state.translationText,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = CaribbeanPalette.JuicyAqua,
                            fontSize = 24.sp,
                            lineHeight = 32.sp
                        )
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------------------------------
// Aquatic Shimmering Water Ripple Microphone Section
// ----------------------------------------------------------------------------

@Composable
private fun AquaticShimmeringMicSection(
    phase: AppPhase,
    isHandsFree: Boolean,
    errorMessage: String?,
    soundLevel: Float,
    micGranted: Boolean,
    micBlocked: Boolean,
    onPressStart: () -> Unit,
    onPressEnd: (Long) -> Unit,
    onRequestMicPermission: () -> Unit,
    onOpenAppSettings: () -> Unit
) {
    val isListening = phase is AppPhase.Listening
    val isTranslating = phase == AppPhase.Translating
    val isSpeaking = phase == AppPhase.Speaking

    val micScale by animateFloatAsState(
        targetValue = if (isListening) 1.15f else 1.0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "micScale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(210.dp)
    ) {
        // Concentric Shimmering Water Ripple Waves
        ShimmeringWaterRipplesCanvas(
            active = isListening || isSpeaking,
            soundLevel = soundLevel
        )

        // Deep Ocean Aquatic Mic Orb
        Surface(
            modifier = Modifier
                .size(118.dp)
                .scale(micScale)
                .pointerInput(micGranted) {
                    detectTapGestures(
                        onPress = {
                            if (!micGranted) {
                                onRequestMicPermission()
                            } else {
                                val downAt = System.currentTimeMillis()
                                onPressStart()
                                tryAwaitRelease()
                                onPressEnd(System.currentTimeMillis() - downAt)
                            }
                        }
                    )
                },
            shape = CircleShape,
            color = Color.Transparent,
            shadowElevation = 12.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = when {
                                isListening -> listOf(
                                    CaribbeanPalette.JuicyAqua,
                                    CaribbeanPalette.DeepLagoon,
                                    CaribbeanPalette.NightOcean
                                )
                                isTranslating -> listOf(
                                    CaribbeanPalette.JuicyGold,
                                    Color(0xFF8A6020),
                                    CaribbeanPalette.NightOcean
                                )
                                isSpeaking -> listOf(
                                    CaribbeanPalette.LuminousGreen,
                                    Color(0xFF0C6632),
                                    CaribbeanPalette.NightOcean
                                )
                                else -> listOf(
                                    CaribbeanPalette.DeepLagoon,
                                    Color(0xFF082234),
                                    CaribbeanPalette.NightOcean
                                )
                            }
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                MicrophoneIconGlyph(
                    tint = if (isListening || isTranslating || isSpeaking) CaribbeanPalette.NightOcean else CaribbeanPalette.DriftwoodWhite,
                    modifier = Modifier.size(46.dp)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Dynamic Status Indicator Pill
    StatusBadgeIndicator(phase = phase, isHandsFree = isHandsFree)

    // Error Message Banner
    errorMessage?.let { msg ->
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = CaribbeanPalette.CoralRed.copy(alpha = 0.2f),
            border = BorderStroke(1.dp, CaribbeanPalette.CoralRed.copy(alpha = 0.5f))
        ) {
            Text(
                text = msg,
                style = MaterialTheme.typography.bodySmall,
                color = CaribbeanPalette.DriftwoodWhite,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .widthIn(max = 320.dp)
            )
        }
    }

    // Permission Alert
    if (!micGranted && micBlocked) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Microphone access is needed for voice translation.",
            style = MaterialTheme.typography.bodySmall,
            color = CaribbeanPalette.DriftwoodWhite.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        TextButton(onClick = onOpenAppSettings) {
            Text("Open App Settings", color = CaribbeanPalette.JuicyGold)
        }
    }

    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
private fun StatusBadgeIndicator(phase: AppPhase, isHandsFree: Boolean) {
    val statusText = when (phase) {
        AppPhase.Idle -> "TAP OR HOLD TO SPEAK"
        is AppPhase.Listening -> if (isHandsFree) "LISTENING... TAP TO FINISH" else "LISTENING... RELEASE TO TRANSLATE"
        AppPhase.Translating -> "TRANSLATING WITH GEMINI AI..."
        AppPhase.Speaking -> "SPEAKING TRANSLATION..."
    }

    val badgeColor = when (phase) {
        AppPhase.Idle -> CaribbeanPalette.DeepLagoon
        is AppPhase.Listening -> CaribbeanPalette.JuicyAqua
        AppPhase.Translating -> CaribbeanPalette.JuicyGold
        AppPhase.Speaking -> CaribbeanPalette.LuminousGreen
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = badgeColor.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.4f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(badgeColor)
            )
            Spacer(modifier = Modifier.widthIn(8.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = CaribbeanPalette.DriftwoodWhite
            )
        }
    }
}

@Composable
private fun ShimmeringWaterRipplesCanvas(
    active: Boolean,
    soundLevel: Float
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waterRipples")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waterProgress"
    )

    Canvas(modifier = Modifier.size(210.dp)) {
        val maxRadius = size.minDimension / 2f
        val loudness = if (active) (0.45f + 0.55f * soundLevel) else 0.2f
        val waveCount = 4

        for (i in 0 until waveCount) {
            val p = (progress + i.toFloat() / waveCount) % 1f
            val alpha = ((1f - p) * 0.5f * loudness).coerceIn(0f, 1f)
            val strokeWidth = (2.dp.toPx() + (3.dp.toPx() * (1f - p)))

            drawCircle(
                color = CaribbeanPalette.JuicyAqua.copy(alpha = alpha),
                radius = maxRadius * (0.40f + 0.60f * p),
                style = Stroke(width = strokeWidth)
            )

            drawCircle(
                color = CaribbeanPalette.DeepLagoon.copy(alpha = alpha * 0.6f),
                radius = maxRadius * (0.35f + 0.60f * p),
                style = Stroke(width = strokeWidth * 0.7f)
            )
        }
    }
}

@Composable
private fun MicrophoneIconGlyph(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.085f

        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.35f, h * 0.10f),
            size = Size(w * 0.30f, h * 0.50f),
            cornerRadius = CornerRadius(w * 0.15f)
        )
        drawArc(
            color = tint,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.21f, h * 0.32f),
            size = Size(w * 0.58f, h * 0.44f),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.5f, h * 0.76f),
            end = Offset(w * 0.5f, h * 0.90f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.34f, h * 0.90f),
            end = Offset(w * 0.66f, h * 0.90f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

// ============================================================================
// 6. HELPER EXTENSIONS
// ============================================================================

private fun Context.checkAudioPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

private fun Context.openSettings() {
    startActivity(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        )
    )
}

// ============================================================================
// 7. PREVIEWS
// ============================================================================

@Preview(showBackground = true, name = "MiLingua Caribbean Preview")
@Composable
fun MiLinguaCaribbeanPreview() {
    MiLinguaTheme {
        MiLinguaContent(
            state = MiLinguaUiState(
                direction = TranslationDirection.ENGLISH_TO_SPANISH,
                phase = AppPhase.Idle,
                transcriptText = "",
                translationText = ""
            ),
            soundLevel = 0f,
            micGranted = true,
            micBlocked = false,
            onDirectionChange = {},
            onDirectionToggle = {},
            onPressStart = {},
            onPressEnd = {},
            onRequestMicPermission = {},
            onOpenAppSettings = {}
        )
    }
}

@Preview(showBackground = true, name = "MiLingua Caribbean Translated")
@Composable
fun MiLinguaCaribbeanTranslatedPreview() {
    MiLinguaTheme {
        MiLinguaContent(
            state = MiLinguaUiState(
                direction = TranslationDirection.ENGLISH_TO_SPANISH,
                phase = AppPhase.Idle,
                transcriptText = "Welcome to the Caribbean! How can I help you today?",
                translationText = "¡Bienvenidos al Caribe! ¿Cómo puedo ayudarte hoy?"
            ),
            soundLevel = 0.5f,
            micGranted = true,
            micBlocked = false,
            onDirectionChange = {},
            onDirectionToggle = {},
            onPressStart = {},
            onPressEnd = {},
            onRequestMicPermission = {},
            onOpenAppSettings = {}
        )
    }
}
