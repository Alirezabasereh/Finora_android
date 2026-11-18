package com.finora.app.util

import java.time.Instant
import java.time.ZoneId

/** Jalali (Shamsi) calendar — exact port of the web app's toJalali / fromJalali (epoch 1600). */
object Jalali {
    val monthNames = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )

    data class Ymd(val y: Int, val m: Int, val d: Int)

    fun toJalali(gy: Int, gm: Int, gd: Int): Ymd {
        val gY = gy - 1600
        val gM = gm - 1
        val gD = gd - 1
        val gMonthDays = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        var gDNo = 365 * gY + Math.floorDiv(gY + 3, 4) - Math.floorDiv(gY + 99, 100) + Math.floorDiv(gY + 399, 400)
        for (i in 0 until gM) gDNo += gMonthDays[i]
        if (gM > 1 && ((gy % 4 == 0 && gy % 100 != 0) || gy % 400 == 0)) gDNo++
        gDNo += gD
        var jDNo = gDNo - 79
        val jNp = Math.floorDiv(jDNo, 12053)
        jDNo %= 12053
        var jy = 979 + 33 * jNp + 4 * Math.floorDiv(jDNo, 1461)
        jDNo %= 1461
        if (jDNo >= 366) {
            jy += Math.floorDiv(jDNo - 1, 365)
            jDNo = (jDNo - 1) % 365
        }
        val jMonthDays = intArrayOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30)
        var jm = 0
        while (jm < 11 && jDNo >= jMonthDays[jm]) {
            jDNo -= jMonthDays[jm]
            jm++
        }
        return Ymd(jy, jm + 1, jDNo + 1)
    }

    fun fromJalali(jy: Int, jm: Int, jd: Int): Ymd {
        val jY = jy - 979
        val jM = jm - 1
        val jD = jd - 1
        val jMonthDays = intArrayOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)
        var jDNo = 365 * jY + Math.floorDiv(jY, 33) * 8 + Math.floorDiv(jY % 33 + 3, 4)
        for (i in 0 until jM) jDNo += jMonthDays[i]
        jDNo += jD
        var gDNo = jDNo + 79
        var gy = 1600 + 400 * Math.floorDiv(gDNo, 146097)
        gDNo %= 146097
        var leap = true
        if (gDNo >= 36525) {
            gDNo--
            gy += 100 * Math.floorDiv(gDNo, 36524)
            gDNo %= 36524
            if (gDNo >= 365) gDNo++ else leap = false
        }
        gy += 4 * Math.floorDiv(gDNo, 1461)
        gDNo %= 1461
        if (gDNo >= 366) {
            leap = false
            gDNo--
            gy += Math.floorDiv(gDNo, 365)
            gDNo %= 365
        }
        val gMonthDays = intArrayOf(31, if (leap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        var gm = 0
        while (gm < 12) {
            if (gDNo < gMonthDays[gm]) break
            gDNo -= gMonthDays[gm]
            gm++
        }
        return Ymd(gy, gm + 1, gDNo + 1)
    }

    fun isLeapYear(jy: Int): Boolean {
        val a = jy - (if (jy > 474) 474 else 473)
        return ((a % 2820 + 474 + 38) * 682) % 2816 < 682
    }

    fun daysInMonth(jy: Int, jm: Int): Int = when {
        jm <= 6 -> 31
        jm <= 11 -> 30
        else -> if (isLeapYear(jy)) 30 else 29
    }

    /** getJalaliDateParts(date) — uses the device's local time zone, like the browser. */
    fun parts(epochMillis: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): Ymd {
        val dt = Instant.ofEpochMilli(epochMillis).atZone(zone)
        return toJalali(dt.year, dt.monthValue, dt.dayOfMonth)
    }

    /** jalaliMonthKey(): "1403_06" */
    fun monthKey(jy: Int, jm: Int): String = "${jy}_${jm.toString().padStart(2, '0')}"

    fun currentMonthKey(): String {
        val p = parts()
        return monthKey(p.y, p.m)
    }
}
