package com.example.scoretrack.util

import com.example.scoretrack.model.Match
import com.example.scoretrack.model.MatchState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// Maç satırının solundaki durum yazısını üretir: "21:45", "67'", "45+", "İY", "MS", "ERT" ...
// Android'e bağımlı değil, böylece birim testiyle denenebilir.
object MatchStatusFormatter {

    // Devre -> (o devreden önceki dakika, devrenin normal bitiş dakikası)
    private val PERIOD_RANGES = mapOf(
        1 to (0 to 45),
        2 to (45 to 90),
        3 to (90 to 105),
        4 to (105 to 120)
    )

    fun statusText(
        match: Match,
        nowMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): String = when (match.state) {
        MatchState.SCHEDULED -> kickoffTime(match.kickoffMillis, timeZone)
        MatchState.POSTPONED -> match.statusLabel ?: "ERT"
        MatchState.FINISHED -> match.statusLabel ?: "MS"
        MatchState.LIVE -> match.statusLabel ?: liveMinute(match.periodId, match.periodStart, nowMillis)
    }

    fun kickoffTime(kickoffMillis: Long, timeZone: TimeZone = TimeZone.getDefault()): String {
        val format = SimpleDateFormat("HH:mm", Locale.US)
        format.timeZone = timeZone
        return format.format(Date(kickoffMillis))
    }

    fun liveMinute(periodId: Int?, periodStart: Long?, nowMillis: Long): String {
        val range = PERIOD_RANGES[periodId]
        if (range == null || periodStart == null || periodStart <= 0L) return "CANLI"

        val elapsed = ((nowMillis - periodStart) / 60_000L).toInt() + 1
        val minute = range.first + elapsed.coerceAtLeast(1)
        return if (minute > range.second) "${range.second}+" else "$minute'"
    }

    // Skorun altındaki küçük satır: "İY 1-0 · Pen 5-3"
    fun extraInfo(match: Match): String? {
        val parts = mutableListOf<String>()
        // İlk yarı skoru: maç bittiyse ya da ikinci yarı/uzatma/penaltılara geçildiyse.
        // Devre arasında (periodId 10) zaten skor = İY skoru, tekrar yazmıyoruz.
        val showHalfTime = match.state == MatchState.FINISHED ||
                (match.state == MatchState.LIVE && match.periodId in 2..5)
        if (showHalfTime && match.halfTimeScore != null) {
            parts += "İY ${match.halfTimeScore}"
        }
        match.penaltyScore?.let { parts += "Pen $it" }
        return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
    }
}
