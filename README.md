# DroidDeck 📱

Your phone as a pocket PC. DroidDeck is a feature-packed Android toolkit that turns any Android 7.0+ device into a proper power tool — file management, app management, network diagnostics, a Wi-Fi file server for your PC, and quick system controls.

## Features

### Dashboard
- Full device info: model, Android version, security patch, CPU cores/architecture, screen resolution & DPI
- **Live RAM monitor** with usage bar
- Storage usage breakdown with progress bar
- Battery level + temperature
- Uptime clock
- Local IP addresses + public IP lookup

### Files
- Browse your entire internal storage
- Open files with any app, share, or delete (recursive)
- Create new folders on the fly
- Uses the "All files access" permission for full visibility

### Apps
- Every installed app listed with icon, package name, version and APK size
- Instant search/filter
- **Export any app's APK straight to Downloads** (share it, back it up, keep it forever)
- Launch apps directly, open system App Info, or trigger uninstall

### Tools
- **Web file server** — flip one switch and browse/download your phone's entire storage from any PC browser on the same Wi-Fi at `http://<phone-ip>:8080` (runs as a foreground service with notification)
- Flashlight toggle
- Vibration test
- Screen brightness slider (with Write Settings grant flow)
- Ping utility (4-round-trip output)
- MD5 / SHA-256 text hasher
- Quick settings shortcuts: Wi-Fi, Bluetooth, Display, Battery Saver, Apps
- Notification permission helper

## Install

Grab `DroidDeck-v1.0.0.apk` from [Releases](https://github.com/justsadnyx-ux/DroidDeck/releases/latest) and sideload it.

- Requires Android 7.0+ (API 24)
- Signed release build (`SHA-256: 9f149398d69418dfc267a03f512393f257156a36b9ce005c81202603170d1f9e`)
- On first use of each feature, grant: All files access (Files tab), Modify system settings (brightness slider), Notifications (server)

## Permissions explained

| Permission | Why |
|---|---|
| `MANAGE_EXTERNAL_STORAGE` | File manager + web server read access |
| `QUERY_ALL_PACKAGES` | Full installed-app list |
| `INTERNET` / `ACCESS_NETWORK_STATE` / `ACCESS_WIFI_STATE` | Web server, public IP, Wi-Fi info |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_DATA_SYNC` | Keeps the file server alive |
| `CAMERA` | Flashlight torch |
| `VIBRATE` | Vibration tool |
| `WRITE_SETTINGS` | Brightness slider |
| `POST_NOTIFICATIONS` | Server status notification |

## Build from source

```bash
gradle assembleRelease
```

Built with Java 17+, Android Gradle Plugin 8.5.2, compileSdk 34, minSdk 24.

## License

MIT
