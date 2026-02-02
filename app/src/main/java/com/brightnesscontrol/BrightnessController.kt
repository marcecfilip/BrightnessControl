package com.brightnesscontrol

import android.content.Context
import android.provider.Settings
import kotlin.math.roundToInt

class BrightnessController(private val context: Context) {

    companion object {
        private const val MIN_BRIGHTNESS = 0
        private const val MAX_BRIGHTNESS = 255
    }

    /**
     * Get current system brightness value (0-100)
     */
    fun getCurrentBrightness(): Int {
        return try {
            val brightness = Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS
            )
            // Convert from 0-255 to 0-100
            ((brightness.toFloat() / MAX_BRIGHTNESS) * 100).roundToInt()
        } catch (e: Settings.SettingNotFoundException) {
            50 // Default to 50% if unable to read
        }
    }

    /**
     * Set system brightness (0-100)
     */
    fun setBrightness(percentage: Int) {
        try {
            // Clamp value between 0 and 100
            val clampedPercentage = percentage.coerceIn(0, 100)
            
            // Convert from 0-100 to 0-255
            val brightnessValue = ((clampedPercentage.toFloat() / 100) * MAX_BRIGHTNESS).roundToInt()
            
            // Update system brightness setting
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                brightnessValue
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Check if app has permission to modify system settings
     */
    fun canModifySettings(): Boolean {
        return Settings.System.canWrite(context)
    }
}
