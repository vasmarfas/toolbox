package com.vasmarfas.card.tools.time

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.periodUntil
import kotlinx.datetime.plus

class LivedTime(
    val years: Int,
    val months: Int,
    val days: Int,
    val hours: Int,
    val minutes: Int,
    val seconds: Int,
    val totalMonths: Int,
    val totalWeeks: Long,
    val totalDays: Long,
    val totalHours: Long,
    val totalMinutes: Long,
    val totalSeconds: Long,
)

class BirthdayCountdown(
    val date: LocalDate,
    val days: Long,
    val hours: Long,
    val minutes: Long,
    val seconds: Long,
    val age: Int,
)

object Age {
    fun parse(text: String): LocalDate? = runCatching { LocalDate.parse(text.trim()) }.getOrNull()

    fun parseTime(hour: String, minute: String): LocalTime? {
        val h = hour.trim().ifEmpty { "0" }.toIntOrNull() ?: return null
        val m = minute.trim().ifEmpty { "0" }.toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        return LocalTime(h, m)
    }

    fun secondsBetween(from: LocalDateTime, to: LocalDateTime): Long =
        from.date.daysUntil(to.date) * 86_400L + (to.time.toSecondOfDay() - from.time.toSecondOfDay())

    fun lived(birth: LocalDateTime, now: LocalDateTime): LivedTime? {
        val total = secondsBetween(birth, now)
        if (total < 0) return null
        val reached = if (now.time < birth.time) now.date.minus(1, DateTimeUnit.DAY) else now.date
        val period = birth.date.periodUntil(reached)
        val rest = total % 86_400
        return LivedTime(
            years = period.years,
            months = period.months,
            days = period.days,
            hours = (rest / 3600).toInt(),
            minutes = (rest % 3600 / 60).toInt(),
            seconds = (rest % 60).toInt(),
            totalMonths = period.years * 12 + period.months,
            totalWeeks = total / 604_800,
            totalDays = total / 86_400,
            totalHours = total / 3600,
            totalMinutes = total / 60,
            totalSeconds = total,
        )
    }

    fun untilBirthday(birth: LocalDateTime, now: LocalDateTime): BirthdayCountdown {
        var target = LocalDateTime(nextBirthday(birth.date, now.date), birth.time)
        if (target < now) target = LocalDateTime(nextBirthday(birth.date, now.date.plus(1, DateTimeUnit.DAY)), birth.time)
        val left = secondsBetween(now, target)
        return BirthdayCountdown(
            date = target.date,
            days = left / 86_400,
            hours = left % 86_400 / 3600,
            minutes = left % 3600 / 60,
            seconds = left % 60,
            age = target.date.year - birth.date.year,
        )
    }

    fun isBirthday(birth: LocalDate, date: LocalDate): Boolean = birthdayIn(birth, date.year) == date

    fun nextBirthday(birth: LocalDate, today: LocalDate): LocalDate {
        val thisYear = birthdayIn(birth, today.year)
        return if (thisYear >= today) thisYear else birthdayIn(birth, today.year + 1)
    }

    private fun birthdayIn(birth: LocalDate, year: Int): LocalDate =
        if (birth.month == Month.FEBRUARY && birth.day == 29 && !isLeap(year)) LocalDate(year, 2, 28) else LocalDate(year, birth.month, birth.day)

    private fun isLeap(year: Int): Boolean = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0
}
