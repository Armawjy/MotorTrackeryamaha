package com.example.motortrackeryamaha.ui.statistics

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.motortrackeryamaha.R

class SimpleBarChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val barPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.accent_blue)
        style = Paint.Style.FILL
    }
    
    private val textPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.text_secondary)
        textSize = 30f
        textAlign = Paint.Align.CENTER
    }

    private var data = listOf<Double>()
    private var labels = listOf<String>()

    fun setData(newData: List<Double>, newLabels: List<String>) {
        data = newData
        labels = newLabels
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (data.isEmpty()) return

        val maxVal = data.maxOrNull()?.takeIf { it > 0 } ?: 1.0
        val barWidth = width / (data.size * 1.5f)
        val spacing = (width - (barWidth * data.size)) / (data.size + 1)

        data.forEachIndexed { i, value ->
            val barHeight = (value / maxVal) * (height - 80f)
            val left = spacing + i * (barWidth + spacing)
            val top = (height - 40f) - barHeight.toFloat()
            val right = left + barWidth
            val bottom = height - 40f
            
            canvas.drawRect(left, top, right, bottom, barPaint)
            
            if (i < labels.size) {
                canvas.drawText(labels[i], left + barWidth / 2, height - 10f, textPaint)
            }
        }
    }
}
