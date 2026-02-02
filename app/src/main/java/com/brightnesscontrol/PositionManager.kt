package com.brightnesscontrol

import android.content.Context
import android.content.SharedPreferences

class PositionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "overlay_position_prefs"
        private const val KEY_POSITION_X = "position_x"
        private const val KEY_POSITION_Y = "position_y"
        private const val DEFAULT_X = -1 // Will be calculated as right edge
        private const val DEFAULT_Y = 0 // Will be calculated as center
    }

    /**
     * Save overlay position
     */
    fun savePosition(x: Int, y: Int) {
        prefs.edit().apply {
            putInt(KEY_POSITION_X, x)
            putInt(KEY_POSITION_Y, y)
            apply()
        }
    }

    /**
     * Get saved X position
     */
    fun getPositionX(): Int {
        return prefs.getInt(KEY_POSITION_X, DEFAULT_X)
    }

    /**
     * Get saved Y position
     */
    fun getPositionY(): Int {
        return prefs.getInt(KEY_POSITION_Y, DEFAULT_Y)
    }

    /**
     * Reset to default position
     */
    fun resetPosition() {
        prefs.edit().clear().apply()
    }

    /**
     * Check if position has been saved before
     */
    fun hasCustomPosition(): Boolean {
        return prefs.contains(KEY_POSITION_X) && prefs.contains(KEY_POSITION_Y)
    }
}
