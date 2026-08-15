package com.example.motortrackeryamaha.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FormatUtils {
    fun formatDistance(distance: Double): String {
        return String.format(Locale.getDefault(), "%.1f KM", distance)
    }

    fun formatDate(date: Long): String {
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        return sdf.format(Date(date))
    }
}
