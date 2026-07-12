package com.example.scoretrack

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.scoretrack.ui.MatchViewModel

class MatchDetailActivity : AppCompatActivity() {

    private val viewModel: MatchViewModel by viewModels()
    private lateinit var contentContainer: LinearLayout
    private var fixtureId: Int = -1
    private var currentTab = "EVENTS"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ana Tasarım
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F4F6F9"))
            fitsSystemWindows = true
            setPadding(0, 48, 0, 0)
        }

        // Sekmeler (Olaylar - Kadrolar)
        val tabLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 16, 0, 16)
            setBackgroundColor(Color.WHITE)
            elevation = 8f
        }

        val btnEvents = createTabButton("Maç Olayları", true)
        val btnLineups = createTabButton("Kadrolar", false)

        tabLayout.addView(btnEvents)
        tabLayout.addView(btnLineups)
        mainLayout.addView(tabLayout)

        // İçerik Alanı
        val scrollView = ScrollView(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT) }
        contentContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 40, 40, 60) }
        scrollView.addView(contentContainer)
        mainLayout.addView(scrollView)
        setContentView(mainLayout)

        fixtureId = intent.getIntExtra("FIXTURE_ID", -1)

        // Tıklama Olayları ve Geçici Ekranlar (Placeholder)
        btnEvents.setOnClickListener {
            currentTab = "EVENTS"
            resetTabs(btnEvents, btnLineups)
            renderPlaceholder("Maç detayları (Goller, Kartlar) yeni altyapıya entegre ediliyor...")
        }

        btnLineups.setOnClickListener {
            currentTab = "LINEUPS"
            resetTabs(btnLineups, btnEvents)
            renderPlaceholder("Kadrolar ve saha dizilişleri yeni altyapıya entegre ediliyor...")
        }

        // İlk açılışta boş ekran görünmesin
        renderPlaceholder("Maç verileri yükleniyor...")

        viewModel.error.observe(this) { msg ->
            if (!msg.isNullOrEmpty()) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun createTabButton(text: String, isSelected: Boolean): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 16f
            setTypeface(null, if (isSelected) Typeface.BOLD else Typeface.NORMAL)
            setTextColor(if (isSelected) Color.parseColor("#2ECC71") else Color.parseColor("#7F8C8D"))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setPadding(0, 32, 0, 32)
        }
    }

    private fun resetTabs(selected: TextView, unselected: TextView) {
        selected.setTypeface(null, Typeface.BOLD)
        selected.setTextColor(Color.parseColor("#2ECC71"))
        unselected.setTypeface(null, Typeface.NORMAL)
        unselected.setTextColor(Color.parseColor("#7F8C8D"))
    }

    // ESKİ ÇÖKEN KODLARIN YERİNE, ŞIK BİR BİLGİ MESAJI BASIYORUZ
    private fun renderPlaceholder(message: String) {
        contentContainer.removeAllViews()
        val textView = TextView(this).apply {
            text = message
            textSize = 16f
            setTypeface(null, Typeface.ITALIC)
            setTextColor(Color.parseColor("#7F8C8D"))
            gravity = Gravity.CENTER
            setPadding(0, 120, 0, 0)
        }
        contentContainer.addView(textView)
    }
}