package com.example.scoretrack.data

import com.example.scoretrack.model.MatchLineups
import com.example.scoretrack.model.PitchPlayer
import com.example.scoretrack.model.SquadPlayer
import com.example.scoretrack.model.TeamLineup
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

// Mackolik maç sayfasının HTML'inden kadroları çıkarır.
// Kadrolar JSON olarak gelmiyor, sayfanın içinde hazır HTML olarak duruyor.
object LineupParser {

    // Saha çiziminin SVG ölçüleri: saha 16px kenar boşluklu, 800 x 524
    private const val FIELD_OFFSET = 16f
    private const val FIELD_LENGTH = 800f
    private const val FIELD_WIDTH = 524f

    // Kadrolar açıklanmadıysa null döner
    fun parse(html: String): MatchLineups? {
        val doc = Jsoup.parse(html)

        val pitch = doc.select(".widget-starting-eleven g.Opta-Node").mapNotNull { node ->
            val circle = node.selectFirst("circle") ?: return@mapNotNull null
            val cx = circle.attr("cx").toFloatOrNull() ?: return@mapNotNull null
            val cy = circle.attr("cy").toFloatOrNull() ?: return@mapNotNull null
            PitchPlayer(
                isHome = node.hasClass("Opta-Home"),
                number = node.selectFirst(".Opta-JerseyNumber")?.text().orEmpty(),
                name = node.selectFirst(".Opta-PlayerName")?.text().orEmpty(),
                x = ((cx - FIELD_OFFSET) / FIELD_LENGTH).coerceIn(0f, 1f),
                y = ((cy - FIELD_OFFSET) / FIELD_WIDTH).coerceIn(0f, 1f)
            )
        }.distinctBy { it.isHome to it.number } // aynı çizim sayfada iki kez geçebiliyor

        val formations = doc.select(".widget-starting-eleven .Opta-TeamFormation").map { it.text() }

        // Sayfada 3 tane "match-stats" bloğu var: ilk 11, yedekler, teknik direktör
        val blocks = doc.select(".widget-match-stats")
        val startersBlock = blocks.firstOrNull { it.attr("data-settings").contains("starting-formation") }
        val subsBlock = blocks.firstOrNull { it.attr("data-settings").contains("substitutions") }
        val managerBlock = blocks.firstOrNull { it.attr("data-settings").contains("manager") }

        if (pitch.isEmpty() && startersBlock == null) return null

        fun team(side: String, formation: String?) = TeamLineup(
            formation = formation,
            coach = managerBlock?.let { rows(it, side).firstOrNull()?.name },
            starters = startersBlock?.let { rows(it, side) }.orEmpty(),
            substitutes = subsBlock?.let { rows(it, side) }.orEmpty()
        )

        return MatchLineups(
            home = team("home", formations.getOrNull(0)),
            away = team("away", formations.getOrNull(1)),
            pitch = pitch
        )
    }

    private fun rows(block: Element, side: String): List<SquadPlayer> =
        block.select(".widget-match-stats__team--$side .widget-match-stats__row").map { row ->
            SquadPlayer(
                number = row.selectFirst(".widget-match-stats__cell--shirt-number")?.text().orEmpty(),
                name = row.selectFirst(".widget-match-stats__person-name")?.text().orEmpty(),
                position = row.selectFirst(".widget-match-stats__cell--person-position")?.text()
                    ?.takeIf { it != "-" }.orEmpty(),
                notes = row.select(".widget-match-stats__icon").mapNotNull { eventText(it) }.joinToString("  ")
            )
        }

    // <span class="widget-match-stats__icon widget-match-stats__icon--goal"> x2 </span> -> "⚽x2"
    private fun eventText(icon: Element): String? {
        val type = icon.classNames().firstOrNull { it.startsWith("widget-match-stats__icon--") }
            ?.removePrefix("widget-match-stats__icon--") ?: return null
        val time = icon.selectFirst(".widget-match-stats__event-time")?.text().orEmpty()
        val symbol = when (type) {
            "goal", "penalty-goal" -> "⚽"
            "own-goal" -> "⚽(KK)"
            "yellow-card" -> "🟨"
            "red-card" -> "🟥"
            "yellow-red-card", "second-yellow-card" -> "🟨🟥"
            "sub-off" -> "↓"
            "sub-on" -> "↑"
            else -> return null
        }
        return symbol + time
    }
}