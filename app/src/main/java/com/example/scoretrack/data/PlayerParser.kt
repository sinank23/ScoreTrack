package com.example.scoretrack.data

import com.example.scoretrack.model.CareerRow
import com.example.scoretrack.model.CareerSeason
import com.example.scoretrack.model.PlayerInfo
import com.example.scoretrack.model.PlayerProfile
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.util.Calendar
import java.util.Locale

// Mackolik oyuncu sayfasının HTML'inden profil ve kariyer bilgilerini çıkarır.
object PlayerParser {

    private const val INFO = ".p0c-person-information"
    private const val CELL = "p0c-person-career-summary__body-cell"

    private val TURKISH_MONTHS = listOf(
        "ocak", "şubat", "mart", "nisan", "mayıs", "haziran",
        "temmuz", "ağustos", "eylül", "ekim", "kasım", "aralık"
    )

    fun parse(html: String): PlayerProfile? {
        val doc = Jsoup.parse(html)
        val info = doc.selectFirst(INFO) ?: return null

        fun text(selector: String) = info.selectFirst(selector)?.text()?.trim()?.takeIf { it.isNotEmpty() }

        // Boy / Kilo / Ayak kutuları: etiket -> değer
        val stats = info.select("${INFO}__stat").associate { stat ->
            stat.selectFirst("${INFO}__label")?.text()?.trim()?.removeSuffix(":")?.trim().orEmpty() to
                    stat.selectFirst("${INFO}__value")?.text()?.trim().orEmpty()
        }

        val birthParts = info.select("${INFO}__date-of-birth").map { it.text().trim() }
        val birthDate = birthParts.joinToString(" ").takeIf { it.isNotBlank() }

        val playerInfo = PlayerInfo(
            name = text("${INFO}__full-name") ?: doc.title().substringBefore(" - ").trim(),
            photo = info.selectFirst("${INFO}__person-photo")?.absUrlOrSrc().orEmpty(),
            shirtNumber = text("${INFO}__shirt-number"),
            position = info.selectFirst("${INFO}__element ${INFO}__label")?.text()?.trim(),
            clubName = text("${INFO}__club-name"),
            clubLogo = info.selectFirst("${INFO}__team-crest")?.absUrlOrSrc(),
            nationality = text("${INFO}__nationality"),
            flag = info.selectFirst("${INFO}__national-flag")?.absUrlOrSrc(),
            birthDate = birthDate,
            age = ageFrom(birthParts),
            birthPlace = text("${INFO}__place-of-birth"),
            height = stats["Boy"]?.takeIf { it.isNotEmpty() },
            weight = stats["Kilo"]?.takeIf { it.isNotEmpty() },
            foot = stats["Ayak"]?.takeIf { it.isNotEmpty() }
        )

        val seasonTable = doc.selectFirst(".widget-player-career-summaries__summary--season table")
        val teamTable = doc.selectFirst(".widget-player-career-summaries__summary--team table")

        return PlayerProfile(
            info = playerInfo,
            teams = teamTable?.let { careerRows(it) }.orEmpty(),
            seasons = seasonTable?.let { seasons(it) }.orEmpty()
        )
    }

    // Sezon tablosu: önce "2026/2027" başlık satırı, sonra o sezonun turnuva satırları, en son "Toplam"
    private fun seasons(table: Element): List<CareerSeason> {
        val result = mutableListOf<CareerSeason>()
        var currentSeason: String? = null
        var currentRows = mutableListOf<CareerRow>()

        for (tr in table.select("tbody > tr")) {
            when {
                tr.hasClass("p0c-person-career-summary__season") -> {
                    // Aynı başlık mobil için iki kez yazılıyor, ikincisini atla
                    if (tr.hasClass("p0c-person-career-summary__season--bp-580")) continue
                    currentSeason?.let { result += CareerSeason(it, currentRows) }
                    currentSeason = tr.text().trim()
                    currentRows = mutableListOf()
                }
                tr.hasClass("p0c-person-career-summary__row") -> {
                    if (currentSeason != null) careerRow(tr)?.let { currentRows += it }
                }
            }
        }
        currentSeason?.let { result += CareerSeason(it, currentRows) }
        return result
    }

    private fun careerRows(table: Element): List<CareerRow> =
        table.select("tbody > tr.p0c-person-career-summary__row").mapNotNull { careerRow(it) }

    private fun careerRow(tr: Element): CareerRow? {
        fun num(name: String) = tr.selectFirst(".$CELL--$name")?.text()?.trim()?.toIntOrNull() ?: 0
        val isTotal = tr.hasClass("p0c-person-career-summary__row--total")
        val teamCell = tr.selectFirst(".$CELL--team")
        if (!isTotal && teamCell == null && tr.selectFirst(".$CELL--competition") == null) return null

        return CareerRow(
            competition = tr.selectFirst(".$CELL--competition .p0c-person-career-summary__competition-name")
                ?.text()?.trim(),
            team = if (isTotal) "Toplam"
            else teamCell?.selectFirst(".p0c-person-career-summary__team-name")?.text()?.trim(),
            teamLogo = teamCell?.selectFirst("img")?.attr("src"),
            appearances = num("appearances"),
            goals = num("goals"),
            assists = num("assists"),
            yellowCards = num("yellow-cards"),
            redCards = num("red-cards"),
            starts = num("first-eleven"),
            isTotal = isTotal
        )
    }

    // "//file.mackolikfeeds.com/..." gibi protokolsüz adresleri https'e çevirir
    private fun Element.absUrlOrSrc(): String {
        val src = attr("src")
        return if (src.startsWith("//")) "https:$src" else src
    }

    // ["09", "Haziran", "2005"] -> yaş
    private fun ageFrom(parts: List<String>): Int? {
        if (parts.size < 3) return null
        val day = parts[0].toIntOrNull() ?: return null
        val month = TURKISH_MONTHS.indexOf(parts[1].lowercase(Locale("tr", "TR"))).takeIf { it >= 0 } ?: return null
        val year = parts[2].toIntOrNull() ?: return null

        val today = Calendar.getInstance()
        var age = today.get(Calendar.YEAR) - year
        val beforeBirthday = today.get(Calendar.MONTH) < month ||
                (today.get(Calendar.MONTH) == month && today.get(Calendar.DAY_OF_MONTH) < day)
        if (beforeBirthday) age--
        return age
    }
}