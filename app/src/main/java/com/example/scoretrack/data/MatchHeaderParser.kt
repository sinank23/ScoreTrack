package com.example.scoretrack.data

import com.example.scoretrack.model.LiveHeader
import org.jsoup.Jsoup

// matchHeader'dan gelen küçük HTML parçasından skor ve dakikayı okur.
object MatchHeaderParser {

    fun parse(html: String): LiveHeader? {
        val doc = Jsoup.parseBodyFragment(html)
        val wrapper = doc.selectFirst("[data-match-status]") ?: return null

        return LiveHeader(
            state = wrapper.attr("data-match-status"),
            homeScore = doc.selectFirst("[data-slot=score-home]")?.text().orEmpty(),
            awayScore = doc.selectFirst("[data-slot=score-away]")?.text().orEmpty(),
            status = doc.selectFirst("[data-slot=status-box]")?.text()?.replace(" ", "").orEmpty(),
            detail = doc.selectFirst(".p0c-soccer-match-details-header__detailed-score")?.text()
                ?.removePrefix("(")?.removeSuffix(")")?.takeIf { it.isNotBlank() }
        )
    }
}