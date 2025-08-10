# 📱 Bluetooth Chat

A simple Android app built with **Jetpack Compose** that discovers nearby devices via Bluetooth and lets users chat in real time.  
The app supports **device discovery**, **connection**, and **bi-directional messaging** with a clean Compose UI.

---

## ✨ Features
- 🔍 Discover nearby Bluetooth devices
- 📡 Connect & chat in real-time via Bluetooth
- 📱 Optimized for on-screen keyboard (messages stay in view)
- 🎨 Modern Jetpack Compose UI

---

## 🛠 Tech Stack
- **Kotlin** – Programming language
- **Jetpack Compose** – UI framework
- **BluetoothAdapter** – Android Bluetooth API
- **Material3** – Theming

---

## 📋 Requirements
- Android 8.0 (API 26) or higher
- Bluetooth hardware support
- Bluetooth permissions granted:
  - `BLUETOOTH_CONNECT`
  - `BLUETOOTH_SCAN`
  - `BLUETOOTH_ADVERTISE` (if required for your Android version)
  - `ACCESS_FINE_LOCATION` (required for device discovery in older versions)
  - `ACCESS_COARSE_LOCATION`

---

## 📱 Application Demo

https://github.com/user-attachments/assets/f7b92416-f589-4ca0-98e7-b5286612e557

---

## 🚀 Getting Started

### 1️⃣ Clone the Repository
```bash
git clone https://github.com/Avaneesh-Chopdekar/bluetooth-chat.git
cd bluetooth-chat
````

### 2️⃣ Open in Android Studio

* Use **Android Studio Giraffe+**
* Sync Gradle files

### 3️⃣ Run the App

* Connect your Android device (Bluetooth enabled)
* Grant permissions when prompted
---

## ⚠️ Notes

* On some devices, you must allow Bluetooth visibility for discovery.
* For Android 12+, Bluetooth permissions must be declared in `AndroidManifest.xml` and requested at runtime.
* If the chat does not work, ensure both devices are paired and in close proximity.

---

## 📜 License

This project is licensed under the MIT License.
