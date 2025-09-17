package com.poria.base.ext

import android.content.res.Resources
import android.graphics.Color
import android.icu.util.Calendar
import android.util.TypedValue
import kotlin.math.abs

fun dp2px(dp: Float) = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP,
    dp,
    Resources.getSystem().displayMetrics
)

fun String.genMacaronColor(): Int {
    val str = this
    val h = (str.hashCode() and Int.MAX_VALUE) % 360          // 0..359
    val s = 35 + (str.hashCode() shr 8) % 16                 // 35-40 %
    val l = 80 + (str.hashCode() shr 16) % 16                // 80-95 %
    return hslToColor(h.toFloat(), s / 100f, l / 100f)
}

fun String.genColor(): Int {
    val str = this
    val h = str.hashCode() % 360  // 0..359
    val s = 55 + (str.hashCode() shr 8) % 20   // 55-75  饱和度
    val l = 65 + (str.hashCode() shr 16) % 15  // 65-80 亮度
    return hslToColor(h.toFloat(), s / 100f, l / 100f)
}

fun hslToColor(h: Float, s: Float, l: Float): Int {
    val c = (1 - abs(2 * l - 1)) * s
    val x = c * (1 - abs(((h / 60) % 2) - 1))
    val m = l - c / 2
    val sector = (h % 360 / 60).toInt().coerceIn(0, 5)
    val (r, g, b) = when (sector) {
        0 -> Triple(c, x, 0f)
        1 -> Triple(x, c, 0f)
        2 -> Triple(0f, c, x)
        3 -> Triple(0f, x, c)
        4 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    val rr = ((r + m) * 255).toInt()
    val gg = ((g + m) * 255).toInt()
    val bb = ((b + m) * 255).toInt()
    return Color.rgb(rr, gg, bb)
}

fun Long.toDateFormat(): DateFormat {
    val cal = Calendar.getInstance().apply {
        timeInMillis = this@toDateFormat
    }
    return DateFormat(
        year = cal.get(Calendar.YEAR),
        month = cal.get(Calendar.MONTH) + 1, // 0→1
        day = cal.get(Calendar.DAY_OF_MONTH),
        hour = cal.get(Calendar.HOUR_OF_DAY),
        minute = cal.get(Calendar.MINUTE),
        second = cal.get(Calendar.SECOND)
    )
}

fun DateFormat.toTimeStamp(): Long {
    val cal = Calendar.getInstance()
    cal.set(year, month - 1, day, hour, minute, second)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

data class DateFormat(
    var year: Int,
    var month: Int,
    var day: Int,
    var hour: Int = 0,
    var minute: Int = 0,
    var second: Int = 0
) {
    override fun toString() = StringBuilder().run {
        append(year).append('-').append(month).append('-').append(day)
        if (hour == 0 && minute == 0 && second == 0) this.toString()
        else if (second == 0) this.append(' ').append(hour)
            .append(':').append(minute).toString()
        else this.append(' ').append(hour)
            .append(':').append(minute)
            .append(':').append(second)
            .toString()
    }
}