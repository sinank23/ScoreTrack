package com.example.scoretrack

import com.example.scoretrack.model.Match
import com.example.scoretrack.model.MatchState
import com.example.scoretrack.util.MatchStatusFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.TimeZone

// Android Studio'da sağ tık > Run 'MatchStatusFormatterTest' ile çalıştırılabilir (emülatör gerekmez).
class MatchStatusFormatterTest {

    private val istanbul = TimeZone.getTimeZone("Europe/Istanbul")
    private val minute = 60_000L

    private fun match(
        state: MatchState,
        label: String? = null,
        periodId: Int? = null,
        periodStart: Long? = null,
        ht: String? = null,
        pen: String? = null
    ) = Match(
        id = "x", competitionId = "c", leagueName = "L", countryName = "", leagueFlag = "", leagueRank = 0,
        homeTeam = "A", awayTeam = "B", homeLogo = "", awayLogo = "",
        homeScore = "1", awayScore = "0",
        kickoffMillis = 1793814300000L, // 4 Kasım 2026 20:45 (İstanbul)
        state = state, statusLabel = label, periodId = periodId, periodStart = periodStart,
        halfTimeScore = ht, penaltyScore = pen, homeRedCards = 0, awayRedCards = 0, sortOrder = 0
    )

    @Test fun scheduledShowsKickoffTime() {
        assertEquals("20:45", MatchStatusFormatter.statusText(match(MatchState.SCHEDULED), 0, istanbul))
    }

    @Test fun firstHalfMinute() {
        val start = 1_000_000L
        assertEquals("1'", MatchStatusFormatter.liveMinute(1, start, start + 10_000))
        assertEquals("23'", MatchStatusFormatter.liveMinute(1, start, start + 22 * minute + 5_000))
        assertEquals("45+", MatchStatusFormatter.liveMinute(1, start, start + 47 * minute))
    }

    @Test fun secondHalfMinute() {
        val start = 1_000_000L
        assertEquals("46'", MatchStatusFormatter.liveMinute(2, start, start + 10_000))
        assertEquals("90+", MatchStatusFormatter.liveMinute(2, start, start + 50 * minute))
    }

    @Test fun mackolikLabelWins() {
        assertEquals("İY", MatchStatusFormatter.statusText(match(MatchState.LIVE, label = "İY", periodId = 10)))
        assertEquals("MS", MatchStatusFormatter.statusText(match(MatchState.FINISHED)))
        assertEquals("ERT", MatchStatusFormatter.statusText(match(MatchState.POSTPONED, label = "ERT")))
    }

    @Test fun extraInfo() {
        assertEquals("İY 1 - 0", MatchStatusFormatter.extraInfo(match(MatchState.FINISHED, ht = "1 - 0")))
        assertNull(MatchStatusFormatter.extraInfo(match(MatchState.LIVE, label = "İY", periodId = 10, ht = "1 - 0")))
        assertEquals(
            "İY 0 - 0 · Pen 5 - 3",
            MatchStatusFormatter.extraInfo(match(MatchState.FINISHED, label = "PEN", ht = "0 - 0", pen = "5 - 3"))
        )
    }
}
