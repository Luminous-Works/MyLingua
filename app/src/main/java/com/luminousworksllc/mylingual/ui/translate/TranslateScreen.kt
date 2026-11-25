package com.luminousworksllc.mylingual.ui.translate

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luminousworksllc.mylingual.model.Language

@Composable
fun TranslateScreen(viewModel: TranslateViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val soundLevel by viewModel.soundLevel.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var micGranted by remember { mutableStateOf(context.hasRecordAudioPermission()) }
    var askedForMic by rememberSaveable { mutableStateOf(false) }
    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        micGranted = granted
        askedForMic = true
    }
    // The user may have changed the permission in system settings, so re-check on return.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        micGranted = context.hasRecordAudioPermission()
    }
    // After one denial, Android still offers a prompt. After a second one (or "Don't ask
    // again"), shouldShowRequestPermissionRationale returns false and only Settings can help.
    val micBlocked = !micGranted &&
        askedForMic &&
        context.findActivity()?.shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) == false

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LanguageSwitch(
            source = state.source,
            enabled = state.phase == Phase.Idle,
            onSelect = viewModel::setSource,
        )

        Spacer(Modifier.height(20.dp))

        TranscriptArea(
            state = state,
            modifier = Modifier.weight(1f),
        )

        MicSection(
            phase = state.phase,
            handsFree = state.handsFree,
            message = state.message,
            soundLevel = soundLevel,
            micGranted = micGranted,
            micBlocked = micBlocked,
            onPressStart = viewModel::onPressStart,
            onPressEnd = viewModel::onPressEnd,
            onRequestMic = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) },
            onOpenSettings = { context.openAppSettings() },
        )
    }
}

/* ---------- Language indicator ---------- */

@Composable
private fun LanguageSwitch(
    source: Language,
    enabled: Boolean,
    onSelect: (Language) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Language.entries.forEachIndexed { index, language ->
            if (index > 0) {
                Text(
                    text = "⇄",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LanguagePill(
                text = language.displayName,
                selected = language == source,
                enabled = enabled,
                onClick = { onSelect(language) },
            )
        }
    }
}

@Composable
private fun LanguagePill(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

/* ---------- Text area ---------- */

@Composable
private fun TranscriptArea(
    state: TranslateUiState,
    modifier: Modifier = Modifier,
) {
    val listening = state.phase is Phase.Listening
    val heard = (state.phase as? Phase.Listening)?.partial ?: state.transcript
    val scroll = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scroll),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (heard.isBlank() && state.translation.isBlank()) {
            Text(
                text = "Press and hold the microphone, then speak.",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        } else {
            if (heard.isNotBlank()) {
                Text(
                    text = heard,
                    style = MaterialTheme.typography.titleLarge,
                    color = if (listening) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(20.dp))
            }
            if (state.translation.isNotBlank()) {
                Text(
                    text = state.translation,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/* ---------- Microphone ---------- */

@Composable
private fun MicSection(
    phase: Phase,
    handsFree: Boolean,
    message: String?,
    soundLevel: Float,
    micGranted: Boolean,
    micBlocked: Boolean,
    onPressStart: () -> Unit,
    onPressEnd: (Long) -> Unit,
    onRequestMic: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val listening = phase is Phase.Listening
    val micScale by animateFloatAsState(
        targetValue = if (listening) 1.08f else 1f,
        label = "micScale",
    )
    val primary = MaterialTheme.colorScheme.primary

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(220.dp),
    ) {
        ListeningRipples(active = listening, level = soundLevel, color = primary)

        Surface(
            modifier = Modifier
                .size(116.dp)
                .scale(micScale)
                // Hold to talk, or quick-tap to latch listening on. See TranslateViewModel.
                .pointerInput(micGranted) {
                    detectTapGestures(
                        onPress = {
                            if (!micGranted) {
                                onRequestMic()
                            } else {
                                val downAt = System.currentTimeMillis()
                                onPressStart()
                                tryAwaitRelease()
                                onPressEnd(System.currentTimeMillis() - downAt)
                            }
                        },
                    )
                },
            shape = CircleShape,
            color = if (listening) primary else MaterialTheme.colorScheme.primaryContainer,
            shadowElevation = 6.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                MicGlyph(
                    tint = if (listening) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    },
                    modifier = Modifier.size(44.dp),
                )
            }
        }
    }

    Spacer(Modifier.height(16.dp))

    Text(
        text = statusText(phase, handsFree),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )

    message?.let {
        Spacer(Modifier.height(8.dp))
        Text(
            text = it,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 320.dp),
        )
    }

    if (!micGranted) {
        Spacer(Modifier.height(8.dp))
        if (micBlocked) {
            Text(
                text = "Microphone access is off for MyLingua.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            TextButton(onClick = onOpenSettings) { Text("Open settings") }
        }
    }

    Spacer(Modifier.height(24.dp))
}

private fun statusText(phase: Phase, handsFree: Boolean): String = when (phase) {
    Phase.Idle -> "Hold to speak"
    is Phase.Listening -> if (handsFree) {
        "Listening. Tap again to finish"
    } else {
        "Listening. Release to translate"
    }
    Phase.Translating -> "Translating…"
    Phase.Speaking -> "Speaking. Press to talk again"
}

/**
 * Soft rings that expand from the microphone while listening. The ring brightness follows the
 * microphone loudness, so the wave reacts to the voice without being distracting.
 */
@Composable
private fun ListeningRipples(
    active: Boolean,
    level: Float,
    color: Color,
) {
    if (!active) return

    val transition = rememberInfiniteTransition(label = "listening")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rippleProgress",
    )

    Canvas(modifier = Modifier.size(220.dp)) {
        val maxRadius = size.minDimension / 2f
        val loudness = 0.55f + 0.45f * level
        val strokeWidth = 2.dp.toPx()
        for (ring in 0 until RING_COUNT) {
            val p = (progress + ring.toFloat() / RING_COUNT) % 1f
            drawCircle(
                color = color.copy(alpha = ((1f - p) * 0.35f * loudness).coerceIn(0f, 1f)),
                radius = maxRadius * (0.45f + 0.55f * p),
                style = Stroke(width = strokeWidth),
            )
        }
    }
}

/** Microphone glyph drawn with primitives, so no extra icon library is needed. */
@Composable
private fun MicGlyph(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.07f

        // Capsule
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.34f, h * 0.10f),
            size = Size(w * 0.32f, h * 0.50f),
            cornerRadius = CornerRadius(w * 0.16f),
        )
        // Cradle: lower half of an ellipse around the capsule
        drawArc(
            color = tint,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.22f, h * 0.30f),
            size = Size(w * 0.56f, h * 0.46f),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        // Stem and base
        drawLine(
            color = tint,
            start = Offset(w * 0.5f, h * 0.76f),
            end = Offset(w * 0.5f, h * 0.90f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.36f, h * 0.90f),
            end = Offset(w * 0.64f, h * 0.90f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

/* ---------- Helpers ---------- */

private const val RING_COUNT = 3

private fun Context.hasRecordAudioPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED

private fun Context.findActivity(): Activity? {
    var context: Context? = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

private fun Context.openAppSettings() {
    startActivity(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null),
        )
    )
}
