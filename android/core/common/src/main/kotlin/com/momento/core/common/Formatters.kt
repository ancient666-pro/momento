package com.momento.core.common

import java.util.Locale
import kotlin.math.abs

fun formatCompactCount(value: Long): String = when {
    abs(value) >= 1_000_000 -> String.format(Locale.US, "%.1fM", value / 1_000_000.0)
    abs(value) >= 1_000 -> String.format(Locale.US, "%.1fK", value / 1_000.0)
    else -> value.toString()
}

fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, secs)
}
