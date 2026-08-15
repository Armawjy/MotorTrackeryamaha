package com.example.motortrackeryamaha.ui.custom

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

class SpeedometerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var currentSpeed = 0f
    private val maxSpeed = 140f
    private val rect = RectF()
    
    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 15f
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#33FFFFFF")
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 15f
        strokeCap = Paint.Cap.ROUND
    }

    private val needlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F44336")
        strokeWidth = 10f
        strokeCap = Paint.Cap.ROUND
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 100f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#9E9E9E")
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }

    fun setSpeed(speed: Float) {
        currentSpeed = speed.coerceIn(0f, maxSpeed)
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = (minOf(width, height) / 2f) - 40f

        rect.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius)
        
        // Draw outer arc
        canvas.drawArc(rect, 135f, 270f, false, arcPaint)

        // Draw progress arc with gradient
        val sweepAngle = (currentSpeed / maxSpeed) * 270f
        val shader = SweepGradient(centerX, centerY, 
            intArrayOf(Color.parseColor("#00E5FF"), Color.parseColor("#FF6D00"), Color.parseColor("#FF6D00")),
            floatArrayOf(0f, 0.5f, 1f)
        )
        val matrix = Matrix()
        matrix.setRotate(135f, centerX, centerY)
        shader.setLocalMatrix(matrix)
        progressPaint.shader = shader
        
        canvas.drawArc(rect, 135f, sweepAngle, false, progressPaint)

        // Draw numbers
        for (i in 0..140 step 20) {
            val angle = 135f + (i / 140f) * 270f
            val rad = Math.toRadians(angle.toDouble())
            val x = centerX + (radius - 50f) * cos(rad).toFloat()
            val y = centerY + (radius - 50f) * sin(rad).toFloat()
            canvas.drawText(i.toString(), x, y + 10f, labelPaint)
        }

        // Draw Needle
        val needleAngle = 135f + sweepAngle
        val needleRad = Math.toRadians(needleAngle.toDouble())
        val needleX = centerX + (radius - 15f) * cos(needleRad).toFloat()
        val needleY = centerY + (radius - 15f) * sin(needleRad).toFloat()
        canvas.drawLine(centerX, centerY, needleX, needleY, needlePaint)
        
        canvas.drawCircle(centerX, centerY, 18f, needlePaint)
        canvas.drawCircle(centerX, centerY, 8f, Paint().apply { color = Color.BLACK })

        // Speed Center Text
        canvas.drawText(currentSpeed.toInt().toString(), centerX, centerY + 80f, textPaint)
        canvas.drawText("km/h", centerX, centerY + 120f, labelPaint)
    }
}
