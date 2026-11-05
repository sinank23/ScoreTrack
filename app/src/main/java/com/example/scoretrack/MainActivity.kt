package com.example.scoretrack

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.scoretrack.ui.MatchAdapter
import com.example.scoretrack.ui.MatchViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.materialswitch.MaterialSwitch
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class MainActivity : AppCompatActivity() {

    private val viewModel: MatchViewModel by viewModels()
    private lateinit var matchAdapter: MatchAdapter

    private lateinit var tvSelectedDate: TextView
    private lateinit var tvSelectedDateSub: TextView
    private lateinit var tvMatchCount: TextView
    private lateinit var tvEmpty: TextView
    private lateinit var progress: ProgressBar
    private lateinit var switchLive: MaterialSwitch
    private lateinit var swipeRefresh: SwipeRefreshLayout

    private val turkish = Locale("tr", "TR")

    // Ekran döndürülünce seçili gün kaybolmasın diye kaydediyoruz
    private var selectedCalendar: Calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvSelectedDate = findViewById(R.id.tvSelectedDate)
        tvSelectedDateSub = findViewById(R.id.tvSelectedDateSub)
        tvMatchCount = findViewById(R.id.tvMatchCount)
        tvEmpty = findViewById(R.id.tvEmpty)
        progress = findViewById(R.id.progress)
        switchLive = findViewById(R.id.switchLive)
        swipeRefresh = findViewById(R.id.swipeRefresh)

        if (savedInstanceState != null) {
            selectedCalendar.timeInMillis = savedInstanceState.getLong(KEY_DATE, selectedCalendar.timeInMillis)
        }

        setupRecyclerView()
        setupDateNavigation()
        setupLiveSwitch()
        setupSwipeRefresh()
        observeData()
        startAutoRefresh()

        // Ekran döndüğünde ViewModel veriyi zaten tutuyor, tekrar çekmeye gerek yok
        if (savedInstanceState == null || !viewModel.hasLoaded) loadSelectedDate() else updateDateLabels()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(KEY_DATE, selectedCalendar.timeInMillis)
    }

    private fun setupRecyclerView() {
        matchAdapter = MatchAdapter(
            onMatchClick = { match -> MatchDetailActivity.start(this, match) },
            onLeagueClick = { header ->
                StandingsActivity.start(this, header.competitionId, header.leagueName)
            }
        )
        findViewById<RecyclerView>(R.id.rvMatches).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = matchAdapter
        }
    }

    private fun setupDateNavigation() {
        findViewById<View>(R.id.btnPrevDay).setOnClickListener { shiftDay(-1) }
        findViewById<View>(R.id.btnNextDay).setOnClickListener { shiftDay(1) }
        findViewById<View>(R.id.llDateLabel).setOnClickListener { openDatePicker() }
    }

    private fun shiftDay(delta: Int) {
        selectedCalendar.add(Calendar.DAY_OF_MONTH, delta)
        loadSelectedDate()
    }

    private fun openDatePicker() {
        // MaterialDatePicker UTC gece yarısı bekler/döndürür
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(
                selectedCalendar.get(Calendar.YEAR),
                selectedCalendar.get(Calendar.MONTH),
                selectedCalendar.get(Calendar.DAY_OF_MONTH)
            )
        }
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Tarih seç")
            .setSelection(utc.timeInMillis)
            .build()

        picker.addOnPositiveButtonClickListener { selection ->
            val picked = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = selection }
            selectedCalendar = Calendar.getInstance().apply {
                set(
                    picked.get(Calendar.YEAR),
                    picked.get(Calendar.MONTH),
                    picked.get(Calendar.DAY_OF_MONTH)
                )
            }
            loadSelectedDate()
        }
        picker.show(supportFragmentManager, "date_picker")
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setColorSchemeColors(ContextCompat.getColor(this, R.color.accent))
        swipeRefresh.setOnRefreshListener { viewModel.refresh() }
    }

    // Uygulama ekrandayken her 30 saniyede bir:
    //  - bugünün listesinde canlı / başlamak üzere maç varsa Mackolik'ten sessizce yenile
    //  - canlı maçların dakikasını ilerlet
    // Uygulama arka plana geçince (onStop) döngü kendiliğinden durur, geri gelince tekrar başlar.
    private fun startAutoRefresh() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    // Uygulamaya geri dönülünce de ilk iş hemen yenileniyor
                    if (isToday(selectedCalendar) && viewModel.needsAutoRefresh()) {
                        viewModel.refresh(silent = true)
                    }
                    matchAdapter.tickLiveMinutes()
                    delay(AUTO_REFRESH_MS)
                }
            }
        }
    }

    private fun setupLiveSwitch() {
        switchLive.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setLiveOnly(isChecked)
            // Canlı maç sadece bugün olur; başka bir gündeysek bugüne dön
            if (isChecked && !isToday(selectedCalendar)) {
                selectedCalendar = Calendar.getInstance()
                loadSelectedDate()
            }
        }
    }

    private fun loadSelectedDate() {
        updateDateLabels()
        // API tarihi her zaman Latin rakamlarıyla olmalı, o yüzden Locale.US
        val apiDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(selectedCalendar.time)
        viewModel.loadDate(apiDate)
    }

    private fun updateDateLabels() {
        tvSelectedDate.text = relativeDayName(selectedCalendar)
            ?: SimpleDateFormat("d MMMM", turkish).format(selectedCalendar.time)
        tvSelectedDateSub.text = SimpleDateFormat("d MMMM yyyy, EEEE", turkish).format(selectedCalendar.time)
    }

    private fun relativeDayName(calendar: Calendar): String? {
        val today = Calendar.getInstance()
        val diff = daysBetween(today, calendar)
        return when (diff) {
            0 -> "BUGÜN"
            1 -> "YARIN"
            -1 -> "DÜN"
            else -> null
        }
    }

    private fun isToday(calendar: Calendar) = daysBetween(Calendar.getInstance(), calendar) == 0

    private fun daysBetween(from: Calendar, to: Calendar): Int {
        fun startOfDay(c: Calendar) = (c.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 12) // yaz saati kaymalarına karşı öğlen kullanıyoruz
            set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return Math.round((startOfDay(to) - startOfDay(from)) / 86_400_000.0).toInt()
    }

    private fun observeData() {
        viewModel.matches.observe(this) { matches ->
            matchAdapter.submitMatches(matches)
            val liveCount = matches.count { it.isLive }
            tvMatchCount.text = if (matches.isEmpty()) "" else
                if (liveCount > 0 && !switchLive.isChecked) "${matches.size} maç · $liveCount canlı"
                else "${matches.size} maç"
            updateEmptyState()
        }

        viewModel.loading.observe(this) { isLoading ->
            if (!isLoading) swipeRefresh.isRefreshing = false
            // Liste doluyken (yenilemede) ortada dönen çark göstermiyoruz
            progress.visibility =
                if (isLoading && viewModel.matches.value.isNullOrEmpty()) View.VISIBLE else View.GONE
            updateEmptyState()
        }

        viewModel.error.observe(this) { msg ->
            if (!msg.isNullOrEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    private fun updateEmptyState() {
        val isLoading = viewModel.loading.value == true
        val isEmpty = viewModel.matches.value.isNullOrEmpty()
        tvEmpty.visibility = if (!isLoading && isEmpty) View.VISIBLE else View.GONE
        tvEmpty.text = if (switchLive.isChecked) "Şu an oynanan maç yok." else "Bu tarihte maç bulunamadı."
    }

    companion object {
        private const val KEY_DATE = "selected_date"
        private const val AUTO_REFRESH_MS = 30_000L
    }
}