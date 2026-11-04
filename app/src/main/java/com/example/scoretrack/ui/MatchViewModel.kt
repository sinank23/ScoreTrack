package com.example.scoretrack.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scoretrack.data.MackolikRepository
import com.example.scoretrack.model.Match
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.IOException

class MatchViewModel : ViewModel() {

    private val _matches = MutableLiveData<List<Match>>(emptyList())
    val matches: LiveData<List<Match>> get() = _matches

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> get() = _loading

    // Gösterildikten sonra clearError() ile temizlenir, ekran döndüğünde tekrar çıkmasın diye
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    private var allMatches: List<Match> = emptyList()
    private var liveOnly = false
    private var currentDate: String? = null
    private var loadJob: Job? = null

    val hasLoaded: Boolean get() = currentDate != null

    fun loadDate(date: String) {
        // Başka bir güne geçildiyse eski günün maçlarını hemen kaldır (yenilemede listeyi tut)
        if (date != currentDate) {
            allMatches = emptyList()
            publish()
        }
        currentDate = date
        // Hızlı hızlı gün değiştirilirse eski istek iptal edilir, yanlış günün maçları gelmez
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _loading.value = true
            try {
                allMatches = MackolikRepository.fetchMatches(date)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                allMatches = emptyList()
                _error.value = "İnternet bağlantısı kurulamadı. Tekrar deneyin."
            } catch (e: Exception) {
                allMatches = emptyList()
                _error.value = "Maçlar yüklenemedi: ${e.message}"
            } finally {
                // İptal edilen eski istek, yenisinin "yükleniyor" durumunu bozmasın
                if (isActive) _loading.value = false
            }
            publish()
        }
    }

    fun refresh() {
        currentDate?.let { loadDate(it) }
    }

    fun setLiveOnly(enabled: Boolean) {
        liveOnly = enabled
        publish()
    }

    fun clearError() {
        _error.value = null
    }

    private fun publish() {
        _matches.value = if (liveOnly) allMatches.filter { it.isLive } else allMatches
    }
}
