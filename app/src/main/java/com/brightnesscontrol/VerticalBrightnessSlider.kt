package com.brightnesscontrol

import android.content.Context
import android.content.res.Configuration
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.math.min

class VerticalBrightnessSlider @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var progress = 50 // 0-100
    private var onProgressChangeListener: ((Int) -> Unit)? = null
    var isRepositionMode = false // New: allow disabling slider interaction

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 18f  // Reduced from 24f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        setShadowLayer(8f, 0f, 2f, Color.parseColor("#80000000"))
    }

    private lateinit var progressGradient: LinearGradient
    private val cornerRadius = 120f

    init {
        updateColors()
    }

    private fun updateColors() {
        val isDarkMode = (context.resources.configuration.uiMode and 
            Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ - Use dynamic colors from system theme
            val accentColor = context.getColor(android.R.color.system_accent1_300)
            val accentColorLight = context.getColor(android.R.color.system_accent1_200)
            val accentColorDark = context.getColor(android.R.color.system_accent1_400)
            
            backgroundPaint.color = if (isDarkMode) {
                Color.parseColor("#30FFFFFF")
            } else {
                Color.parseColor("#40000000")
            }
            
            strokePaint.color = if (isDarkMode) {
                Color.parseColor("#20FFFFFF")
            } else {
                Color.parseColor("#30000000")
            }
            
            // Store gradient colors for later use
            gradientColors = intArrayOf(accentColorLight, accentColor, accentColorDark)
        } else {
            // Pre-Android 12 - Use teal colors
            backgroundPaint.color = if (isDarkMode) {
                Color.parseColor("#40FFFFFF")
            } else {
                Color.parseColor("#40000000")
            }
            
            strokePaint.color = if (isDarkMode) {
                Color.parseColor("#20FFFFFF")
            } else {
                Color.parseColor("#30000000")
            }
            
            gradientColors = intArrayOf(
                Color.parseColor("#FF80CBC4"),
                Color.parseColor("#FF4DB6AC"),
                Color.parseColor("#FF26A69A")
            )
        }
    }

    private var gradientColors = intArrayOf(
        Color.parseColor("#FF80CBC4"),
        Color.parseColor("#FF4DB6AC"),
        Color.parseColor("#FF26A69A")
    )

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateColors()
        // Create gradient from bottom to top
        progressGradient = LinearGradient(
            0f, h.toFloat(),
            0f, 0f,
            gradientColors,
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        progressPaint.shader = progressGradient
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        val rect = RectF(0f, 0f, w, h)

        // Draw background
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, backgroundPaint)

        // Draw progress fill from bottom
        val progressHeight = (progress / 100f) * h
        val progressRect = RectF(0f, h - progressHeight, w, h)
        
        // Clip to rounded rect for progress
        val path = Path().apply {
            addRoundRect(progressRect, cornerRadius, cornerRadius, Path.Direction.CW)
        }
        canvas.save()
        canvas.clipPath(path)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, progressPaint)
        canvas.restore()

        // Draw stroke
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, strokePaint)

        // Draw percentage text
        val text = "$progress%"
        val textY = h / 2 - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(text, w / 2, textY, textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isRepositionMode) return false // Let parent handle drag when repositioning
        
        when (event.action) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE -> {
                // Calculate progress from touch position (inverted - top = 100%, bottom = 0%)
                val newProgress = ((height - event.y) / height * 100).toInt()
                setProgress(newProgress.coerceIn(0, 100), true)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    fun setProgress(value: Int, fromUser: Boolean = false) {
        val oldProgress = progress
        progress = value.coerceIn(0, 100)
        if (oldProgress != progress) {
            invalidate()
            if (fromUser) {
                onProgressChangeListener?.invoke(progress)
            }
        }
    }

    fun getProgress(): Int = progress

    fun setOnProgressChangeListener(listener: (Int) -> Unit) {
        onProgressChangeListener = listener
    }
}
