package com.example.scoretrack.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scoretrack.data.MackolikRepository
import com.example.scoretrack.model.PlayerProfile
import kotlinx.coroutines.launch
import java.io.IOException

sealed class PlayerState {
    object Loading : PlayerState()
    data class Ready(val profile: PlayerProfile) : PlayerState()
    data class Failed(val message: String) : PlayerState()
}

class PlayerViewModel : ViewModel() {

    private val _state = MutableLiveData<PlayerState>(PlayerState.Loading)
    val state: LiveData<PlayerState> get() = _state

    private var loaded = false

    fun load(profileUrl: String, force: Boolean = false) {
        if (loaded && !force) return
        loaded = true
        _state.value = PlayerState.Loading
        viewModelScope.launch {
            _state.value = try {
                val profile = MackolikRepository.fetchPlayer(profileUrl)
                if (profile == null) PlayerState.Failed("Oyuncu bilgisi bulunamadı.")
                else PlayerState.Ready(profile)
            } catch (e: IOException) {
                loaded = false
                PlayerState.Failed("İnternet bağlantısı kurulamadı.")
            } catch (e: Exception) {
                loaded = false
                PlayerState.Failed("Oyuncu bilgisi yüklenemedi: ${e.message}")
            }
        }
    }
}