package com.luminousworksllc.mylingual

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.sp

/**
 * One look for the whole screen. Light themes use darker accents than dark ones, because
 * [accent] and [gold] are also used as text colors.
 */
data class AppPalette(
    val label: String,
    val isDark: Boolean,
    val background: Color,
    val backgroundMid: Color,
    val accent: Color,
    val accentDeep: Color,
    val gold: Color,
    val text: Color,
    val success: Color,
    val card: Color,
    val translationCard: Color,
    val alert: Color,
    /** Middle ring of the mic orb while idle, translating and speaking. */
    val orbIdle: Color,
    val orbTranslating: Color,
    val orbSpeaking: Color,
    /** Mic glyph color while idle. */
    val micIdleIcon: Color
)

val AppThemes = listOf(
    AppPalette(
        label = "Night Ocean", isDark = true,
        background = Color(0xFF050810), backgroundMid = Color(0xFF081224),
        accent = Color(0xFF00CEC9), accentDeep = Color(0xFF0AB3A8),
        gold = Color(0xFFD4A843), text = Color(0xFFF8EDD0),
        success = Color(0xFF2ECC71), card = Color(0xFF0D1322),
        translationCard = Color(0xFF092032), alert = Color(0xFFC0392B),
        orbIdle = Color(0xFF082234), orbTranslating = Color(0xFF8A6020),
        orbSpeaking = Color(0xFF0C6632), micIdleIcon = Color(0xFFF8EDD0)
    ),
    AppPalette(
        label = "Caribbean Sand", isDark = false,
        background = Color(0xFFF1E2C4), backgroundMid = Color(0xFFF9F0DB),
        accent = Color(0xFF006367), accentDeep = Color(0xFF007C80),
        gold = Color(0xFF9C431F), text = Color(0xFF3A2A18),
        success = Color(0xFF1F7A3D), card = Color(0xFFFFF8E8),
        translationCard = Color(0xFFFFFBF0), alert = Color(0xFFA82E20),
        orbIdle = Color(0xFF3F9C9A), orbTranslating = Color(0xFFC4764F),
        orbSpeaking = Color(0xFF55A06C), micIdleIcon = Color(0xFFFFF8E8)
    ),
    AppPalette(
        label = "Sunny Day", isDark = false,
        background = Color(0xFFFFE04A), backgroundMid = Color(0xFFFFEC85),
        accent = Color(0xFF005F80), accentDeep = Color(0xFF007DA6),
        gold = Color(0xFF00695A), text = Color(0xFF073B54),
        success = Color(0xFF1E7A45), card = Color(0xFFFFF8CC),
        translationCard = Color(0xFFFFFADB), alert = Color(0xFFB3261E),
        orbIdle = Color(0xFF37A7CB), orbTranslating = Color(0xFF2E9E86),
        orbSpeaking = Color(0xFF4BA873), micIdleIcon = Color(0xFFFFFFFF)
    ),
    AppPalette(
        label = "Overcast", isDark = true,
        background = Color(0xFF35414B), backgroundMid = Color(0xFF44515C),
        accent = Color(0xFFA9D6D2), accentDeep = Color(0xFF7FBDB8),
        gold = Color(0xFFE8D3A0), text = Color(0xFFF2F5F7),
        success = Color(0xFFA8DDB0), card = Color(0xFF53616D),
        translationCard = Color(0xFF4B5A66), alert = Color(0xFFE58B80),
        orbIdle = Color(0xFF4F7478), orbTranslating = Color(0xFF8F8062),
        orbSpeaking = Color(0xFF5F8A6B), micIdleIcon = Color(0xFFF2F5F7)
    )
)

/**
 * The colors the screen reads. The names date from the original Night Ocean look; each one
 * now resolves through [current], so changing the theme recomposes everything that uses it.
 */
object CaribbeanPalette {
    var current by mutableStateOf(AppThemes.first())
        private set

    val NightOcean get() = current.background
    val JuicyAqua get() = current.accent
    val DeepLagoon get() = current.accentDeep
    val JuicyGold get() = current.gold
    val DriftwoodWhite get() = current.text
    val LuminousGreen get() = current.success
    val DeepSpaceCard get() = current.card
    val CoralRed get() = current.alert

    private const val PREFS = "appearance"
    private const val KEY_THEME = "theme"

    fun load(context: Context) {
        val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_THEME, null)
        current = AppThemes.firstOrNull { it.label == saved } ?: AppThemes.first()
    }

    fun select(context: Context, theme: AppPalette) {
        current = theme
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_THEME, theme.label).apply()
    }
}

/** Gear button that opens the theme list. */
@Composable
fun ThemeMenuButton(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var open by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        TextButton(
            onClick = { open = true },
            modifier = Modifier.semantics { contentDescription = "Choose theme" }
        ) {
            Text(text = "⚙︎", fontSize = 22.sp, color = CaribbeanPalette.DriftwoodWhite)
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            containerColor = CaribbeanPalette.current.backgroundMid
        ) {
            AppThemes.forEach { theme ->
                val selected = theme == CaribbeanPalette.current
                DropdownMenuItem(
                    text = { Text(if (selected) "✓  ${theme.label}" else "     ${theme.label}") },
                    onClick = {
                        CaribbeanPalette.select(context, theme)
                        open = false
                    }
                )
            }
        }
    }
}
