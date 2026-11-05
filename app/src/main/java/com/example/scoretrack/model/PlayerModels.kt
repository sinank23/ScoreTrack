package com.example.scoretrack.model

// Oyuncu profil sayfasının üst kısmı
data class PlayerInfo(
    val name: String,
    val photo: String,
    val shirtNumber: String?,
    val position: String?,       // "Orta saha"
    val clubName: String?,
    val clubLogo: String?,
    val nationality: String?,
    val flag: String?,
    val birthDate: String?,      // "09 Haziran 2005"
    val age: Int?,
    val birthPlace: String?,
    val height: String?,         // "173 cm"
    val weight: String?,         // "60 kg"
    val foot: String?            // "Sağ"
)

// Kariyer tablolarındaki bir satır (bir turnuva ya da bir takım)
data class CareerRow(
    val competition: String?,    // sezon tablosunda: "Trendyol Süper Lig"
    val team: String?,
    val teamLogo: String?,
    val appearances: Int,
    val goals: Int,
    val assists: Int,
    val yellowCards: Int,
    val redCards: Int,
    val starts: Int,             // ilk 11'de başladığı maç
    val isTotal: Boolean = false
)

data class CareerSeason(
    val season: String,          // "2026/2027"
    val rows: List<CareerRow>,   // son satır "Toplam" olabilir
)

data class PlayerProfile(
    val info: PlayerInfo,
    val teams: List<CareerRow>,          // kariyeri boyunca oynadığı takımlar (+ Toplam)
    val seasons: List<CareerSeason>      // sezon sezon
)