package com.example.scoretrack.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scoretrack.data.MackolikRepository
import com.example.scoretrack.model.LiveHeader
import com.example.scoretrack.model.MatchEvent
import com.example.scoretrack.model.MatchLineups
import kotlinx.coroutines.launch
import java.io.IOException

// Kadrolar sekmesinin durumu
sealed class LineupState {
    object Loading : LineupState()
    object NotAnnounced : LineupState()
    data class Ready(val lineups: MatchLineups) : LineupState()
    data class Failed(val message: String) : LineupState()
}

class MatchDetailViewModel : ViewModel() {

    // null = henüz yükleniyor
    private val _events = MutableLiveData<List<MatchEvent>?>()
    val events: LiveData<List<MatchEvent>?> get() = _events

    // null = henüz istenmedi (kadrolar sekmesine ilk geçişte yüklenir)
    private val _lineups = MutableLiveData<LineupState?>()
    val lineups: LiveData<LineupState?> get() = _lineups

    // Canlı maçta güncel skor ve dakika (null = henüz güncelleme yok, listeden gelen bilgi kullanılır)
    private val _header = MutableLiveData<LiveHeader?>()
    val header: LiveData<LiveHeader?> get() = _header

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    fun loadEvents(matchId: String) {
        viewModelScope.launch {
            try {
                _events.value = MackolikRepository.fetchMatchEvents(matchId)
            } catch (e: IOException) {
                _events.value = emptyList()
                _error.value = "İnternet bağlantısı kurulamadı."
            } catch (e: Exception) {
                _events.value = emptyList()
                _error.value = "Maç olayları yüklenemedi: ${e.message}"
            }
        }
    }

    // Kadro sayfası büyük (~300 KB), o yüzden sadece sekme açılınca ve bir kez indiriyoruz
    fun loadLineupsIfNeeded(matchId: String) {
        val current = _lineups.value
        if (current != null && current !is LineupState.Failed) return

        _lineups.value = LineupState.Loading
        viewModelScope.launch {
            _lineups.value = try {
                val result = MackolikRepository.fetchLineups(matchId)
                if (result == null) LineupState.NotAnnounced else LineupState.Ready(result)
            } catch (e: IOException) {
                LineupState.Failed("İnternet bağlantısı kurulamadı.")
            } catch (e: Exception) {
                LineupState.Failed("Kadrolar yüklenemedi: ${e.message}")
            }
        }
    }

    // Canlı maçta skor/dakika ve olayları sessizce yeniler.
    // Hata olursa mesaj göstermez, ekrandakiyle devam eder.
    fun refreshLive(matchId: String) {
        viewModelScope.launch {
            try {
                MackolikRepository.fetchLiveHeader(matchId)?.let { _header.value = it }
                _events.value = MackolikRepository.fetchMatchEvents(matchId)
            } catch (e: Exception) {
                // sessiz yenileme
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}