package com.finora.app.util

import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor

/** Persian digits / money formatting — a 1:1 port of the web app's helpers (Intl fa-IR). */
object Fa {
    private const val FA_DIGITS = "۰۱۲۳۴۵۶۷۸۹"
    private const val AR_DIGITS = "٠١٢٣٤٥٦٧٨٩"
    private const val GROUP_SEP = '\u066C'
    private const val MINUS = "\u200E\u2212"

    /** toPersianNumber(): replaces every ASCII digit with its Persian form. */
    fun digits(value: Any?): String {
        val s = value.toString()
        val sb = StringBuilder(s.length)
        for (c in s) {
            if (c in '0'..'9') sb.append(FA_DIGITS[c - '0']) else sb.append(c)
        }
        return sb.toString()
    }

    /** normalizeDigitString(): Persian / Arabic-Indic digits -> ASCII digits. */
    fun normalizeDigits(value: String?): String {
        if (value == null) return ""
        val sb = StringBuilder(value.length)
        for (c in value) {
            val p = FA_DIGITS.indexOf(c)
            val a = AR_DIGITS.indexOf(c)
            when {
                p >= 0 -> sb.append(('0' + p))
                a >= 0 -> sb.append(('0' + a))
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    /** JS Math.round (half rounds toward +infinity). */
    fun roundHalfUp(x: Double): Long = floor(x + 0.5).toLong()

    /** Intl.NumberFormat('fa-IR').format(n) for integers. */
    fun group(n: Long): String {
        val neg = n < 0
        val abs = if (neg) (-n).toString() else n.toString()
        val sb = StringBuilder()
        val first = abs.length % 3
        if (first > 0) sb.append(abs, 0, first)
        var i = first
        while (i < abs.length) {
            if (sb.isNotEmpty()) sb.append(GROUP_SEP)
            sb.append(abs, i, i + 3)
            i += 3
        }
        val body = digits(sb.toString())
        return if (neg) MINUS + body else body
    }

    fun plainMoney(amount: Double?): String {
        val x = amount ?: 0.0
        val r = roundHalfUp(x)
        // JS quirk kept on purpose: Math.round(-0.3) is -0 and Intl prints it as "−۰"
        val negativeZero = r == 0L && (x < 0.0 || (x == 0.0 && 1.0 / x < 0.0))
        return if (negativeZero) MINUS + digits("0") else group(r)
    }
    fun plainMoney(amount: Long): String = group(amount)

    fun money(amount: Double?): String = plainMoney(amount) + " تومان"
    fun money(amount: Long): String = group(amount) + " تومان"

    fun percent(value: Double, fractionDigits: Int = 1): String {
        val v = if (value.isNaN() || value.isInfinite()) 0.0 else value
        return digits(String.format(Locale.ROOT, "%.${fractionDigits}f", abs(v))) + "٪"
    }

    /** parseMoneyInput(): accepts numbers or digit strings (any digit script); anything else -> 0. */
    fun parseMoney(value: Any?): Long {
        if (value is Number) {
            val d = value.toDouble()
            return if (d.isNaN() || d.isInfinite()) 0L else roundHalfUp(d)
        }
        if (value == null || value is Boolean) return 0L
        val cleaned = normalizeDigits(value.toString()).filter { it.isDigit() && it in '0'..'9' || it == '-' }
        if (cleaned.isEmpty() || cleaned == "-") return 0L
        return cleaned.toLongOrNull() ?: 0L
    }
}
