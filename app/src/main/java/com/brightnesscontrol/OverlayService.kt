package com.brightnesscontrol

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.content.res.Resources
import android.os.Handler
import android.os.Looper
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.database.ContentObserver
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.NotificationCompat

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private lateinit var brightnessController: BrightnessController
    private lateinit var positionManager: PositionManager
    private lateinit var params: WindowManager.LayoutParams
    private var isOverlayShown = false
    
    private val hideHandler = Handler(Looper.getMainLooper())
    private var isUserInteracting = false
    private var isRepositionMode = false
    private var extraDimObserver: ContentObserver? = null

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "overlay_service_channel"
        private const val AUTO_HIDE_DELAY = 3000L // 3 seconds
        const val ACTION_RESET_POSITION = "com.brightnesscontrol.RESET_POSITION"
        const val ACTION_ENABLE_REPOSITION_MODE = "com.brightnesscontrol.ENABLE_REPOSITION_MODE"
        const val ACTION_DISABLE_REPOSITION_MODE = "com.brightnesscontrol.DISABLE_REPOSITION_MODE"
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        brightnessController = BrightnessController(this)
        positionManager = PositionManager(this)
        createNotificationChannel()
        setupExtraDimObserver()
    }

    private fun setupExtraDimObserver() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            extraDimObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    if (isOverlayShown) {
                        val extraDimButton = overlayView.findViewById<com.google.android.material.button.MaterialButton>(R.id.extraDimButton)
                        updateExtraDimIcon(extraDimButton)
                    }
                }
            }
            contentResolver.registerContentObserver(
                Settings.Secure.getUriFor("reduce_bright_colors_activated"),
                false,
                extraDimObserver!!
            )
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_RESET_POSITION -> {
                positionManager.resetPosition()
                if (isOverlayShown) {
                    updateOverlayPosition()
                }
            }
            ACTION_ENABLE_REPOSITION_MODE -> {
                isRepositionMode = true
                if (isOverlayShown) {
                    val brightnessSlider = overlayView.findViewById<VerticalBrightnessSlider>(R.id.brightnessSlider)
                    brightnessSlider.isRepositionMode = true
                    showOverlayVisibility()
                    cancelAutoHide()
                }
            }
            ACTION_DISABLE_REPOSITION_MODE -> {
                isRepositionMode = false
                if (isOverlayShown) {
                    val brightnessSlider = overlayView.findViewById<VerticalBrightnessSlider>(R.id.brightnessSlider)
                    brightnessSlider.isRepositionMode = false
                    scheduleAutoHide()
                }
            }
            else -> {
                if (!isOverlayShown) {
                    showOverlay()
                    startForeground(NOTIFICATION_ID, createNotification())
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun showOverlay() {
        // Inflate the overlay layout with a Material theme context
        val themedContext = ContextThemeWrapper(this, R.style.Theme_BrightnessControl)
        overlayView = LayoutInflater.from(themedContext).inflate(R.layout.overlay_layout, null)

        // Get screen dimensions
        val displayMetrics = Resources.getSystem().displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        // Load saved position or use default (right side)
        val savedX = positionManager.getPositionX()
        val savedY = positionManager.getPositionY()
        
        val defaultX = screenWidth - 68 // Right edge with 20dp margin (48dp width + 20dp margin)
        val defaultY = (screenHeight - 280) / 2 // Vertically centered (280dp height)

        val finalX = if (positionManager.hasCustomPosition()) savedX else defaultX
        val finalY = if (positionManager.hasCustomPosition()) savedY else defaultY

        // Set up window parameters with explicit dimensions
        val widthPx = (48 * displayMetrics.density).toInt()  // 48dp in pixels
        
        params = WindowManager.LayoutParams(
            widthPx,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = finalX
            y = finalY
        }

        // Add view to window manager
        windowManager.addView(overlayView, params)
        isOverlayShown = true

        // Set up UI components
        setupOverlayUI()
        
        // Set up draggable functionality
        setupDraggable()
        
        // Start auto-hide timer
        scheduleAutoHide()
    }

    private fun setupOverlayUI() {
        val brightnessSlider = overlayView.findViewById<VerticalBrightnessSlider>(R.id.brightnessSlider)
        val extraDimButton = overlayView.findViewById<com.google.android.material.button.MaterialButton>(R.id.extraDimButton)

        // Initialize brightness
        val currentBrightness = brightnessController.getCurrentBrightness()
        brightnessSlider.setProgress(currentBrightness)
        brightnessSlider.isRepositionMode = isRepositionMode

        // Brightness slider listener - only active when not in reposition mode
        brightnessSlider.setOnProgressChangeListener { progress ->
            if (!isRepositionMode) {
                brightnessController.setBrightness(progress)
                showOverlayTemporarily()
            }
        }

        // Extra Dim toggle logic
        updateExtraDimIcon(extraDimButton)
        extraDimButton.setOnClickListener {
            if (!isRepositionMode) {
                toggleExtraDim()
                updateExtraDimIcon(extraDimButton)
                showOverlayTemporarily()
            }
        }
    }

    private fun updateExtraDimIcon(button: com.google.android.material.button.MaterialButton) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val isExtraDimActive = Settings.Secure.getInt(
                contentResolver,
                "reduce_bright_colors_activated",
                0
            ) == 1
            
            button.alpha = if (isExtraDimActive) 1.0f else 0.4f
            // Optional: change icon based on state
            button.setIconResource(if (isExtraDimActive) android.R.drawable.ic_menu_agenda else android.R.drawable.ic_menu_agenda)
        } else {
            button.visibility = View.GONE
        }
    }

    private fun toggleExtraDim() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val isExtraDimActive = Settings.Secure.getInt(
                contentResolver,
                "reduce_bright_colors_activated",
                0
            ) == 1
            
            val newValue = if (isExtraDimActive) 0 else 1

            try {
                // Attempt to toggle directly if permission is granted via ADB
                val success = Settings.Secure.putInt(
                    contentResolver,
                    "reduce_bright_colors_activated",
                    newValue
                )
                
                if (success) {
                    Toast.makeText(this, if (newValue == 1) "Extra Dim On" else "Extra Dim Off", Toast.LENGTH_SHORT).show()
                } else {
                    openExtraDimSettings()
                }
            } catch (e: SecurityException) {
                // Fallback to settings if permission is missing
                openExtraDimSettings()
            } catch (e: Exception) {
                openExtraDimSettings()
            }
        }
    }

    private fun openExtraDimSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val intent = Intent("android.settings.REDUCE_BRIGHT_COLORS_SETTINGS")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                Toast.makeText(this, "Opening Extra Dim settings...", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Extra Dim settings not found", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun setupDraggable() {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        overlayView.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isUserInteracting = true
                    cancelAutoHide()
                    showOverlayVisibility()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(overlayView, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    // Save position when drag ends
                    positionManager.savePosition(params.x, params.y)
                    isUserInteracting = false
                    scheduleAutoHide()
                    
                    // Check if it was a tap (not a drag)
                    val deltaX = Math.abs(event.rawX - initialTouchX)
                    val deltaY = Math.abs(event.rawY - initialTouchY)
                    if (deltaX < 10 && deltaY < 10) {
                        // It was a tap, not a drag - let the SeekBar handle it
                        view.performClick()
                    }
                    true
                }
                else -> false
            }
        }
    }
    
    private fun updateOverlayPosition() {
        if (isOverlayShown) {
            val displayMetrics = Resources.getSystem().displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val screenHeight = displayMetrics.heightPixels
            
            val defaultX = screenWidth - 68 // Right edge with margin (48dp + 20dp)
            val defaultY = (screenHeight - 280) / 2 // Vertically centered
            
            params.x = if (positionManager.hasCustomPosition()) {
                positionManager.getPositionX()
            } else {
                defaultX
            }
            params.y = if (positionManager.hasCustomPosition()) {
                positionManager.getPositionY()
            } else {
                defaultY
            }
            windowManager.updateViewLayout(overlayView, params)
        }
    }
    
    private fun showOverlayVisibility() {
        overlayView.alpha = 1f
    }
    
    private fun hideOverlayVisibility() {
        if (!isUserInteracting) {
            overlayView.animate()
                .alpha(0.3f)
                .setDuration(300)
                .start()
        }
    }
    
    private fun showOverlayTemporarily() {
        cancelAutoHide()
        showOverlayVisibility()
        scheduleAutoHide()
    }
    
    private fun scheduleAutoHide() {
        cancelAutoHide()
        hideHandler.postDelayed({
            hideOverlayVisibility()
        }, AUTO_HIDE_DELAY)
    }
    
    private fun cancelAutoHide() {
        hideHandler.removeCallbacksAndMessages(null)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_description)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        extraDimObserver?.let {
            contentResolver.unregisterContentObserver(it)
        }
        if (isOverlayShown) {
            windowManager.removeView(overlayView)
            isOverlayShown = false
        }
    }
}
