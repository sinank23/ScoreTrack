package com.example.scoretrack

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import coil.transform.CircleCropTransformation
import com.example.scoretrack.data.RetrofitClient
import kotlinx.coroutines.launch

class PlayerProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#2C3E50"))
            fitsSystemWindows = true
            setPadding(48, 64, 48, 64)
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
            addView(mainLayout)
        }
        setContentView(scrollView)

        val playerId = intent.getIntExtra("PLAYER_ID", -1)
        if (playerId == -1) {
            Toast.makeText(this, "Oyuncu bulunamadı!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val tvLoading = TextView(this).apply {
            text = "Oyuncu bilgileri getiriliyor..."
            setTextColor(Color.WHITE)
            textSize = 18f
            gravity = Gravity.CENTER
        }
        mainLayout.addView(tvLoading)

        lifecycleScope.launch {
            try {
                val apiData = RetrofitClient.apiService.getPlayerProfile(playerId)
                val playerData = apiData.response?.firstOrNull()

                mainLayout.removeAllViews()

                if (playerData?.player == null) {
                    mainLayout.addView(TextView(this@PlayerProfileActivity).apply { text = "Detay bulunamadı."; setTextColor(Color.WHITE) })
                    return@launch
                }

                val p = playerData.player
                val stats = playerData.statistics?.firstOrNull()

                val ivPhoto = ImageView(this@PlayerProfileActivity).apply {
                    layoutParams = LinearLayout.LayoutParams(300, 300).apply { setMargins(0, 0, 0, 32) }
                    load(p.photo) {
                        crossfade(true)
                        transformations(CircleCropTransformation())
                    }
                }

                val tvName = TextView(this@PlayerProfileActivity).apply {
                    text = p.name ?: "Bilinmiyor"
                    textSize = 28f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(Color.WHITE)
                    gravity = Gravity.CENTER
                }

                val detailsCard = LinearLayout(this@PlayerProfileActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(48, 48, 48, 48)
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 48, 0, 0) }
                    background = GradientDrawable().apply { shape = GradientDrawable.RECTANGLE; cornerRadius = 32f; setColor(Color.WHITE) }
                    elevation = 16f
                }

                val addRow = { title: String, value: String ->
                    val row = LinearLayout(this@PlayerProfileActivity).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 16, 0, 16) }
                    val tvTitle = TextView(this@PlayerProfileActivity).apply { text = title; textSize = 16f; setTextColor(Color.parseColor("#7F8C8D")); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
                    val tvValue = TextView(this@PlayerProfileActivity).apply { text = value; textSize = 16f; setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor("#2C3E50")); gravity = Gravity.END }
                    row.addView(tvTitle); row.addView(tvValue)
                    detailsCard.addView(row)
                }

                addRow("Uyruk", p.nationality ?: "?")
                addRow("Yaş", "${p.age ?: "?"} yaşında")
                addRow("Boy / Kilo", "${p.height ?: "?"} / ${p.weight ?: "?"}")

                detailsCard.addView(View(this@PlayerProfileActivity).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2).apply { setMargins(0, 24, 0, 24) }; setBackgroundColor(Color.parseColor("#E0E0E0")) })

                addRow("Güncel Takım", stats?.team?.name ?: "?")
                addRow("Mevki", stats?.games?.position ?: "?")
                addRow("Oynadığı Maç", "${stats?.games?.appearences ?: 0}")
                addRow("Gol / Asist", "⚽ ${stats?.goals?.total ?: 0}  |  🎯 ${stats?.goals?.assists ?: 0}")

                mainLayout.addView(ivPhoto)
                mainLayout.addView(tvName)
                mainLayout.addView(detailsCard)

            } catch (e: Exception) {
                tvLoading.text = "Hata oluştu kanka: ${e.message}"
            }
        }
    }
}