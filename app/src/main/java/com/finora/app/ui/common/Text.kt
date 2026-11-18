package com.finora.app.ui.common

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.finora.app.ui.theme.FaRegular
import com.finora.app.ui.theme.FaSolid
import com.finora.app.ui.theme.Finora
import com.finora.app.ui.theme.Vazir
import com.finora.app.ui.theme.normalLineHeight

/** Vazirmatn text. `size` is in CSS px (= dp = sp here, the app pins fontScale to 1). */
@Composable
fun FText(
    text: String,
    size: Float,
    modifier: Modifier = Modifier,
    weight: FontWeight = FontWeight.Normal,
    color: Color = Finora.colors.text,
    align: TextAlign = TextAlign.Unspecified,
    lineHeight: Float = normalLineHeight(size),
    letterSpacing: Float = 0f,
    maxLines: Int = Int.MAX_VALUE,
    softWrap: Boolean = true
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = TextStyle(
            color = color,
            fontSize = size.sp,
            fontWeight = weight,
            fontFamily = Vazir,
            lineHeight = lineHeight.sp,
            letterSpacing = letterSpacing.sp,
            textAlign = align
        ),
        overflow = TextOverflow.Clip,
        softWrap = softWrap,
        maxLines = maxLines
    )
}

/** Font Awesome glyph (same font + codepoints as the web app's <i class="fa-solid ...">). */
@Composable
fun FaIcon(
    glyph: String,
    size: Float,
    modifier: Modifier = Modifier,
    color: Color = Finora.colors.text,
    regular: Boolean = false
) {
    BasicText(
        text = glyph,
        modifier = modifier,
        style = TextStyle(
            color = color,
            fontSize = size.sp,
            fontFamily = if (regular) FaRegular else FaSolid,
            fontWeight = FontWeight.Normal,
            lineHeight = size.sp
        ),
        softWrap = false,
        maxLines = 1
    )
}
