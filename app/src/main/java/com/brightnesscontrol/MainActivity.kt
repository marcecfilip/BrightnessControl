package com.brightnesscontrol

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.brightnesscontrol.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isServiceRunning = false

    companion object {
        private const val REQUEST_OVERLAY_PERMISSION = 1001
        private const val REQUEST_SETTINGS_PERMISSION = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        updatePermissionStatus()
        setupToggleButton()
        setupResetPositionButton()
        setupRepositionModeButton()
    }

    private var isRepositionMode = false

    private fun setupRepositionModeButton() {
        binding.repositionModeButton.setOnClickListener {
            isRepositionMode = !isRepositionMode
            
            val intent = Intent(this, OverlayService::class.java).apply {
                action = if (isRepositionMode) {
                    OverlayService.ACTION_ENABLE_REPOSITION_MODE
                } else {
                    OverlayService.ACTION_DISABLE_REPOSITION_MODE
                }
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            
            // Update button appearance
            binding.repositionModeButton.text = if (isRepositionMode) {
                "Disable Reposition Mode"
            } else {
                getString(R.string.reposition_mode)
            }
            
            Toast.makeText(
                this,
                if (isRepositionMode) R.string.reposition_mode_on else R.string.reposition_mode_off,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun setupResetPositionButton() {
        binding.resetPositionButton.setOnClickListener {
            // Send broadcast to reset position
            val intent = Intent(this, OverlayService::class.java).apply {
                action = OverlayService.ACTION_RESET_POSITION
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            Toast.makeText(this, R.string.position_reset, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupToggleButton() {
        binding.toggleButton.setOnClickListener {
            if (isServiceRunning) {
                stopOverlayService()
            } else {
                startOverlayService()
            }
        }
    }

    private fun startOverlayService() {
        // Check overlay permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            requestOverlayPermission()
            return
        }

        // Check settings permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(this)) {
            requestSettingsPermission()
            return
        }

        // Start the service
        val intent = Intent(this, OverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        
        isServiceRunning = true
        updateToggleButton()
        Toast.makeText(this, "Overlay started", Toast.LENGTH_SHORT).show()
    }

    private fun stopOverlayService() {
        val intent = Intent(this, OverlayService::class.java)
        stopService(intent)
        isServiceRunning = false
        updateToggleButton()
        Toast.makeText(this, "Overlay stopped", Toast.LENGTH_SHORT).show()
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION)
        }
    }

    private fun requestSettingsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_WRITE_SETTINGS,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, REQUEST_SETTINGS_PERMISSION)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        when (requestCode) {
            REQUEST_OVERLAY_PERMISSION -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                    updatePermissionStatus()
                    // Try to start service again if settings permission is also granted
                    if (Settings.System.canWrite(this)) {
                        startOverlayService()
                    } else {
                        requestSettingsPermission()
                    }
                } else {
                    Toast.makeText(this, "Overlay permission required", Toast.LENGTH_LONG).show()
                }
            }
            REQUEST_SETTINGS_PERMISSION -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.System.canWrite(this)) {
                    updatePermissionStatus()
                    // Try to start service again if overlay permission is also granted
                    if (Settings.canDrawOverlays(this)) {
                        startOverlayService()
                    } else {
                        requestOverlayPermission()
                    }
                } else {
                    Toast.makeText(this, "Settings permission required", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
    }

    private fun updatePermissionStatus() {
        val overlayGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }

        val settingsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.System.canWrite(this)
        } else {
            true
        }

        binding.overlayPermissionStatus.text = if (overlayGranted) {
            "✓ Overlay permission granted"
        } else {
            "✗ Overlay permission required"
        }

        binding.settingsPermissionStatus.text = if (settingsGranted) {
            "✓ Settings permission granted"
        } else {
            "✗ Settings permission required"
        }

        val secureSettingsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        binding.secureSettingsStatus.text = if (secureSettingsGranted) {
            "✓ Direct Extra Dim toggle: Enabled"
        } else {
            "✗ Direct Extra Dim toggle: Not Enabled"
        }
    }

    private fun updateToggleButton() {
        binding.toggleButton.text = if (isServiceRunning) {
            getString(R.string.stop_overlay)
        } else {
            getString(R.string.start_overlay)
        }
    }
}
