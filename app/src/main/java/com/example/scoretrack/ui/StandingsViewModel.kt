package com.example.scoretrack.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scoretrack.data.MackolikRepository
import com.example.scoretrack.model.LeagueTable
import kotlinx.coroutines.launch
import java.io.IOException

sealed class StandingsState {
    object Loading : StandingsState()
    object NoTable : StandingsState()          // kupa gibi, puan tablosu olmayan turnuvalar
    data class Ready(val table: LeagueTable) : StandingsState()
    data class Failed(val message: String) : StandingsState()
}

class StandingsViewModel : ViewModel() {

    private val _state = MutableLiveData<StandingsState>(StandingsState.Loading)
    val state: LiveData<StandingsState> get() = _state

    private var loaded = false

    // Sayfa büyük (~1 MB), o yüzden ekran dönse bile bir kez indiriyoruz
    fun load(competitionId: String, force: Boolean = false) {
        if (loaded && !force) return
        loaded = true
        _state.value = StandingsState.Loading
        viewModelScope.launch {
            _state.value = try {
                val table = MackolikRepository.fetchStandings(competitionId)
                if (table == null) StandingsState.NoTable else StandingsState.Ready(table)
            } catch (e: IOException) {
                loaded = false
                StandingsState.Failed("İnternet bağlantısı kurulamadı.")
            } catch (e: Exception) {
                loaded = false
                StandingsState.Failed("Puan durumu yüklenemedi: ${e.message}")
            }
        }
    }
}