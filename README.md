# BrightnessControl

**A Lightweight Brightness Utility for Android devices without an auto-brightness feature**

![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat-square&logo=android&logoColor=white)
![Language](https://img.shields.io/badge/Language-Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)

## 📖 Overview
**BrightnessControl** is a native Android utility engineered in **Kotlin** designed to solve a specific hardware limitation: the lack of granular or automated brightness control on specialized tablets and embedded displays. By leveraging system-level overlays, it provides a persistent, low-overhead interface for luminosity management.

---

## 🚀 Key Features

* **Persistent Overlay:** Utilizing the `SYSTEM_ALERT_WINDOW` permission to provide an "always-on-top" slider for instant access during high-intensity workflows.
* **Granular Precision:** Fine-tune screen luminosity beyond standard system increments, essential for late-night coding or color-sensitive work.
* **Material Design 3:** A clean interface that adheres to modern Android design standards for a seamless user experience.

---

## 🛠 Technical Implementation

* **Language:** 100% Kotlin
* **API Level:** Optimized for **API 21+** to ensure compatibility across legacy and modern ARM-based hardware.
* **Key Permissions:** * `WRITE_SETTINGS`: To modify system-level hardware parameters.
    * `ACTION_MANAGE_OVERLAY_PERMISSION`: To maintain the floating UI layer.

---

## 📥 Installation

### Prerequisites
* Android device running version 5.0 (Lollipop) or higher.
* "Install from Unknown Sources" enabled for APK deployment.

### From Source
```bash
# Clone the repository
git clone https://github.com/marcecfilip/BrightnessControl.git
cd BrightnessControl

# Build the debug APK via Gradle
./gradlew assembleDebug

# Deploy to device via ADB
adb install app/build/outputs/apk/debug/app-debug.apk
