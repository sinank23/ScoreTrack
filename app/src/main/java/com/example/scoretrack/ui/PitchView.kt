package com.example.scoretrack.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.scoretrack.R
import com.example.scoretrack.model.PitchPlayer

// Dikey yeşil saha: ev sahibi aşağıda, deplasman yukarıda.
// Oyuncuların yerleri Mackolik'in kendi diziliş çiziminden geliyor.
class PitchView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var players: List<PitchPlayer> = emptyList()
    private var homeLabel = ""
    private var awayLabel = ""

    private val density = resources.displayMetrics.density
    private fun dp(v: Float) = v * density

    private val grassDark = Paint().apply { color = Color.parseColor("#2E7D32") }
    private val grassLight = Paint().apply { color = Color.parseColor("#388E3C") }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(170, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = dp(1.5f)
    }
    private val homePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.accent)
    }
    private val awayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1565C0") }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = dp(1.5f)
    }
    private val numberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = dp(11f)
        typeface = Typeface.DEFAULT_BOLD
    }
    private val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = dp(10f)
        setShadowLayer(dp(2f), 0f, dp(1f), Color.BLACK)
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_primary)
        textAlign = Paint.Align.CENTER
        textSize = dp(13f)
        typeface = Typeface.DEFAULT_BOLD
    }

    fun setData(players: List<PitchPlayer>, homeLabel: String, awayLabel: String) {
        this.players = players
        this.homeLabel = homeLabel
        this.awayLabel = awayLabel
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = (width * 1.5f).toInt()
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val labelHeight = dp(28f)
        val field = RectF(dp(4f), labelHeight, width - dp(4f), height - labelHeight)
        val fw = field.width()
        val fh = field.height()

        // Takım etiketleri
        canvas.drawText(awayLabel, width / 2f, labelHeight - dp(9f), labelPaint)
        canvas.drawText(homeLabel, width / 2f, height - dp(9f), labelPaint)

        // Çim şeritleri
        val stripes = 12
        for (i in 0 until stripes) {
            val top = field.top + fh * i / stripes
            val bottom = field.top + fh * (i + 1) / stripes
            canvas.drawRect(field.left, top, field.right, bottom, if (i % 2 == 0) grassDark else grassLight)
        }

        // Saha çizgileri
        val cx = field.centerX()
        val cy = field.centerY()
        canvas.drawRect(field, linePaint)
        canvas.drawLine(field.left, cy, field.right, cy, linePaint)
        canvas.drawCircle(cx, cy, fw * 0.135f, linePaint)

        val boxW = fw * 0.59f
        val boxH = fh * 0.157f
        val smallW = fw * 0.27f
        val smallH = fh * 0.052f
        // Üst ceza sahası
        canvas.drawRect(cx - boxW / 2, field.top, cx + boxW / 2, field.top + boxH, linePaint)
        canvas.drawRect(cx - smallW / 2, field.top, cx + smallW / 2, field.top + smallH, linePaint)
        // Alt ceza sahası
        canvas.drawRect(cx - boxW / 2, field.bottom - boxH, cx + boxW / 2, field.bottom, linePaint)
        canvas.drawRect(cx - smallW / 2, field.bottom - smallH, cx + smallW / 2, field.bottom, linePaint)

        // Oyuncular
        val radius = dp(13f)
        val maxNameWidth = fw / 4.5f
        for (p in players) {
            // Mackolik'in yatay sahasını 90° çevirip dikey yapıyoruz
            val px = field.left + p.y * fw
            val py = field.top + (1f - p.x) * fh

            canvas.drawCircle(px, py, radius, if (p.isHome) homePaint else awayPaint)
            canvas.drawCircle(px, py, radius, ringPaint)
            canvas.drawText(p.number, px, py + numberPaint.textSize / 3f, numberPaint)

            val name = shortName(p.name, maxNameWidth)
            canvas.drawText(name, px, py + radius + namePaint.textSize + dp(2f), namePaint)
        }
    }

    // "Abdülkerim Bardakcı" sığmazsa "Bardakcı" yaz
    private fun shortName(name: String, maxWidth: Float): String {
        if (namePaint.measureText(name) <= maxWidth) return name
        return name.substringAfterLast(' ').ifEmpty { name }
    }
}