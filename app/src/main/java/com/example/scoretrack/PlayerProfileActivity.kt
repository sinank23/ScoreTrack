package com.example.scoretrack

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class PlayerProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ana Tasarım
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F4F6F9"))
            fitsSystemWindows = true
            gravity = Gravity.CENTER
            setPadding(60, 60, 60, 60)
        }

        // Şık Bir Bilgi Mesajı (Placeholder)
        val tvPlaceholder = TextView(this).apply {
            text = "Oyuncu profilleri, gol ve istatistik verileri yeni Mackolik altyapısına taşınıyor...\n\nÇok yakında aktif olacak!"
            textSize = 18f
            setTypeface(null, Typeface.ITALIC)
            setTextColor(Color.parseColor("#7F8C8D"))
            gravity = Gravity.CENTER
        }

        mainLayout.addView(tvPlaceholder)
        setContentView(mainLayout)
    }
}