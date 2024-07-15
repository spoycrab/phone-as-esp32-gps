# Use your phone as a GPS for your ESP32 Project

This project demonstrates how an Android application can automatically send GPS location information to an ESP32. The app collects location data from the Android device and sends it to the ESP32 via HTTP POST requests. This project is useful if you want to use GPS information on your ESP32 but you don't have access to a GPS module.

## Table of Contents

- [Installation](#installation)
- [Configuration](#configuration)
- [Usage](#usage)

## Installation

### Android Application

1. Clone this repository:
   ```sh
   git clone https://github.com/your-username/repository-name.git
2. Open the project in Android Studio.
3. Connect your Android device via USB or set up an emulator.
4. Build and install the app on your device.

### ESP32

1. Connect the ESP32 to your computer.
2. Open Arduino IDE.
3. Ensure you have the ESP32 library installed.
4. Upload the ESP32 code available in the "esp32gps" folder of this repository.
5. In the ESP32 code, configure the SSID and password of the Wi-Fi network to ensure the ESP32 is connected to the same network as the Android device.

## Configuration

- Permissions: Ensure the app has the necessary permissions to access location and the internet.
- Network Configuration: Configure the network_security_config.xml file if necessary to allow HTTP traffic.

## Usage

1. Run the ESP32 code and copy the IP displayed on your serial monitor.
2. Run the App on your android device and enter the IP displayed on your ESP32.
3. Press the "start device" button to start sending GPS information to your ESP32.
- The app will automatically start sending GPS location data to the ESP32 every second even if your phone is idle.
- Use the "Stop" button in the app to stop sending location data.
