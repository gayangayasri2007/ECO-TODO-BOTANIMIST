package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    private val isoDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val isoTimeFormatter = SimpleDateFormat("HH:mm", Locale.US)
    private val time12Formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val time24Formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val displayDateFormatter = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
    private val fullDisplayDateFormatter = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())

    fun todayIso(): String {
        return isoDateFormatter.format(Date())
    }

    fun currentTimeIso(): String {
        return isoTimeFormatter.format(Date())
    }

    fun parseDate(iso: String): Date? {
        return try {
            isoDateFormatter.parse(iso)
        } catch (e: Exception) {
            null
        }
    }

    fun offsetDate(iso: String, days: Int): String {
        val cal = Calendar.getInstance()
        val d = parseDate(iso) ?: Date()
        cal.time = d
        cal.add(Calendar.DAY_OF_YEAR, days)
        return isoDateFormatter.format(cal.time)
    }

    fun formatDisplayDate(iso: String): String {
        val today = todayIso()
        val yesterday = offsetDate(today, -1)
        val tomorrow = offsetDate(today, 1)

        return when (iso) {
            today -> "Today"
            yesterday -> "Yesterday"
            tomorrow -> "Tomorrow"
            else -> {
                val d = parseDate(iso) ?: return iso
                displayDateFormatter.format(d)
            }
        }
    }

    fun formatFullDate(iso: String): String {
        val d = parseDate(iso) ?: return iso
        return fullDisplayDateFormatter.format(d)
    }

    fun formatTime(isoTime: String, use24Hour: Boolean = false): String {
        return try {
            val d = isoTimeFormatter.parse(isoTime) ?: return isoTime
            if (use24Hour) time24Formatter.format(d) else time12Formatter.format(d)
        } catch (e: Exception) {
            isoTime
        }
    }

    /**
     * Returns the 7 days (Monday through Sunday) for the week containing the given date.
     */
    fun getWeekDays(referenceIso: String): List<String> {
        val cal = Calendar.getInstance()
        val d = parseDate(referenceIso) ?: Date()
        cal.time = d
        cal.firstDayOfWeek = Calendar.MONDAY
        // Set to Monday
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = (dayOfWeek - Calendar.MONDAY + 7) % 7
        cal.add(Calendar.DAY_OF_YEAR, -daysFromMonday)

        val days = mutableListOf<String>()
        for (i in 0 until 7) {
            days.add(isoDateFormatter.format(cal.time))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return days
    }

    fun getDayOfWeekLabel(iso: String): String {
        val d = parseDate(iso) ?: return ""
        val cal = Calendar.getInstance()
        cal.time = d
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Mon"
            Calendar.TUESDAY -> "Tue"
            Calendar.WEDNESDAY -> "Wed"
            Calendar.THURSDAY -> "Thu"
            Calendar.FRIDAY -> "Fri"
            Calendar.SATURDAY -> "Sat"
            Calendar.SUNDAY -> "Sun"
            else -> ""
        }
    }

    fun isPastDate(isoDate: String): Boolean {
        return isoDate < todayIso()
    }

    fun isToday(isoDate: String): Boolean {
        return isoDate == todayIso()
    }
}
