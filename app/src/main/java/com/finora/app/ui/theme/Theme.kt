package com.finora.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.finora.app.R
import kotlin.math.roundToInt

fun rgba(r: Int, g: Int, b: Int, a: Float = 1f): Color = Color(r / 255f, g / 255f, b / 255f, a)

/** Design tokens — copied from the web app's :root (dark) and body.light-mode (light) CSS variables. */
@Immutable
class FinoraColors(
    val isLight: Boolean,
    val bg0: Color,
    val bg1: Color,
    val card: Color,
    val card2: Color,
    val border: Color,
    val text: Color,
    val muted: Color,
    val accent1: Color,
    val accent2: Color,
    val accent3: Color,
    val good: Color,
    val warn: Color,
    val bad: Color,
    /** --l-surface: nested surface on a card (dark: translucent white). */
    val surface: Color,
    /** --l-line-strong (dark: --border). */
    val lineStrong: Color,
    val navBg: Color,
    val navBorder: Color
)

val DarkColors = FinoraColors(
    isLight = false,
    bg0 = Color(0xFF070B17), bg1 = Color(0xFF0B1022),
    card = rgba(255, 255, 255, .07f), card2 = rgba(255, 255, 255, .10f),
    border = rgba(255, 255, 255, .10f),
    text = Color(0xFFF8FAFC), muted = rgba(248, 250, 252, .70f),
    accent1 = Color(0xFF6A5CFF), accent2 = Color(0xFF00D2FF), accent3 = Color(0xFFFF3CAC),
    good = Color(0xFF18E6A6), warn = Color(0xFFFFB84D), bad = Color(0xFFFF5D7D),
    surface = rgba(255, 255, 255, .07f), lineStrong = rgba(255, 255, 255, .10f),
    navBg = Color(0xFF282846), navBorder = rgba(255, 255, 255, .10f)
)

val LightColors = FinoraColors(
    isLight = true,
    bg0 = Color(0xFFE8ECF7), bg1 = Color(0xFFF3F5FB),
    card = Color(0xFFFFFFFF), card2 = Color(0xFFF6F8FD),
    border = rgba(15, 23, 42, .11f),
    text = Color(0xFF0F172A), muted = Color(0xFF566074),
    accent1 = Color(0xFF5A49F2), accent2 = Color(0xFF0A88AD), accent3 = Color(0xFFD6247F),
    good = Color(0xFF0E9F6E), warn = Color(0xFFC2700A), bad = Color(0xFFD92D52),
    surface = Color(0xFFF5F7FC), lineStrong = rgba(15, 23, 42, .16f),
    navBg = rgba(255, 255, 255, .90f), navBorder = rgba(15, 23, 42, .16f)
)

val LocalFinora = staticCompositionLocalOf { DarkColors }

object Finora {
    val colors: FinoraColors
        @Composable @ReadOnlyComposable get() = LocalFinora.current
}

val Vazir = FontFamily(
    Font(R.font.vazirmatn_light, FontWeight.Light),
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_semibold, FontWeight.SemiBold),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
    Font(R.font.vazirmatn_extrabold, FontWeight.ExtraBold),
    Font(R.font.vazirmatn_black, FontWeight.Black)
)

val FaSolid = FontFamily(Font(R.font.fa_solid, FontWeight.Normal))
val FaRegular = FontFamily(Font(R.font.fa_regular, FontWeight.Normal))

/**
 * Chrome's `line-height: normal` for Vazirmatn (hhea ascent 2100 / descent 1100 per 2048 upm),
 * rounded per component exactly like the browser does. Verified against rendered pages.
 */
fun normalLineHeight(size: Float): Float =
    (2100f / 2048f * size).roundToInt() + (1100f / 2048f * size).roundToInt().toFloat()

@Composable
fun FinoraTheme(light: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalFinora provides (if (light) LightColors else DarkColors)) {
        content()
    }
}
