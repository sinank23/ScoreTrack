package com.example.scoretrack.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scoretrack.data.MackolikRepository
import com.example.scoretrack.model.Match
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MatchViewModel : ViewModel() {

    private val _matches = MutableLiveData<List<Match>>()
    val matches: LiveData<List<Match>> get() = _matches

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    fun fetchMatchesByDate(date: String) {
        viewModelScope.launch {
            try {
                val fetchedMatches = withContext(Dispatchers.IO) {
                    MackolikRepository.fetchMatches(date)
                }
                _matches.value = fetchedMatches
            } catch (e: Exception) {
                _error.value = "HATA: ${e.message}"
            }
        }
    }

    fun fetchLiveMatches() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        viewModelScope.launch {
            try {
                val fetchedMatches = withContext(Dispatchers.IO) {
                    MackolikRepository.fetchMatches(today)
                }
                _matches.value = fetchedMatches.filter { it.isLive }
            } catch (e: Exception) {
                _error.value = "HATA: ${e.message}"
            }
        }
    }
}