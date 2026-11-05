package com.example.scoretrack.data

import com.example.scoretrack.model.LeagueTable
import com.example.scoretrack.model.StandingRow
import com.example.scoretrack.model.ZoneLegend
import org.jsoup.Jsoup

// Mackolik puan durumu sayfasının HTML'inden tabloyu çıkarır.
// Sütun sırası: #, (sıra değişimi), Takım, O, +/-, G, B, M, A, Y, Av, P
object StandingsParser {

    private val ZONE_REGEX = Regex("rank-status-(\\d+)")

    // Tablo yoksa (kupa gibi) null döner
    fun parse(html: String): LeagueTable? {
        val doc = Jsoup.parse(html)
        val table = doc.selectFirst("table.p0c-competition-tables__table--total") ?: return null

        val rows = table.select("tr.p0c-competition-tables__row").mapNotNull { tr ->
            val cells = tr.select("> td")
            if (cells.size < 12) return@mapNotNull null
            fun num(i: Int) = cells[i].text().trim().toIntOrNull() ?: 0

            StandingRow(
                rank = num(0),
                teamId = tr.attr("data-team-id"),
                teamName = tr.attr("data-team-name").ifEmpty {
                    cells[2].selectFirst(".p0c-competition-tables__team-name--full")?.text().orEmpty()
                },
                logo = cells[2].selectFirst("img")?.attr("src").orEmpty(),
                played = num(3),
                won = num(5),
                drawn = num(6),
                lost = num(7),
                goalsFor = num(8),
                goalsAgainst = num(9),
                goalDiff = num(10),
                points = num(11),
                zone = ZONE_REGEX.find(tr.className())?.groupValues?.get(1)?.toIntOrNull()
            )
        }
        if (rows.isEmpty()) return null

        val legend = doc.select(".p0c-competition-tables__legend .p0c-competition-tables__rank-status")
            .mapNotNull { item ->
                val zone = Regex("rank-status--(\\d+)").find(item.className())
                    ?.groupValues?.get(1)?.toIntOrNull() ?: return@mapNotNull null
                ZoneLegend(zone, item.text().trim())
            }

        // Sayfa başlığı: "Türkiye Trendyol Süper Lig 2026/2027, İddaa, Puan Durumu, ..." -> ilk kısım
        val title = doc.title().substringBefore(",").trim()

        return LeagueTable(title = title, rows = rows, legend = legend)
    }
}