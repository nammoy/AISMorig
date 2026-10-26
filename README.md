# 20Claude — Android app

Native Android app (Kotlin + Jetpack Compose). This guide explains how to build
and run it on your own computer.

---

## 1. What you need

| Tool | Version |
|---|---|
| [Android Studio](https://developer.android.com/studio) | Narwhal (2025.1) or newer — it includes the JDK 17 the build needs |
| Android SDK | Platform **API 35** (installed from Android Studio's SDK Manager) |
| Device | An Android phone or emulator running **Android 8.0 (API 26)** or newer |
| Internet | Needed for the first build (downloads Gradle and libraries) |

You do **not** need to install Gradle or Java separately — the project ships the
Gradle wrapper (`gradlew`) and Android Studio provides Java.

---

## 2. Get the project

Clone the repository (or unzip the copy you were given):

```sh
git clone <repository-url>
cd AI-ShieldMe-Frontend-android-native
```

---

## 3. Add the configuration files

The app needs two config files. Ask the development team for the
values if you don't have them.

### `local.properties` (project root)

Copy the example file and fill it in:

```sh
cp local.properties.example local.properties
```

```properties
sdk.dir=/path/to/Android/sdk
TWENTYCLAUDE_APP_TOKEN=<app token from the backend team>
```

`sdk.dir` is the Android SDK location. Typical paths:

- **macOS:** `/Users/<you>/Library/Android/sdk`
- **Windows:** `C\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk`
- **Linux:** `/home/<you>/Android/Sdk`

(Android Studio writes this line for you when it opens the project.)

### `app/google-services.json`

The Firebase config file (Firebase project `ai-sheildme-dev`). It must sit at
`app/google-services.json`. Without it the build fails.

---

## 4. Run with Android Studio (recommended)

1. Open Android Studio → **File › Open** → select the project folder.
2. Wait for **Gradle sync** to finish (the first time takes several minutes).
3. If prompted, install any missing SDK components.
4. Pick a device in the toolbar:
   - **Physical phone:** enable *Developer options* → *USB debugging*, then
     connect it by USB and accept the prompt on the phone.
   - **Emulator:** **Tools › Device Manager › Create Device**, choose a phone
     with a **Google Play** system image, API 26 or newer.
5. Press **Run ▶** (or `Shift+F10` / `Ctrl+R` on macOS).

The app installs and launches on the selected device.

---

## 5. Run from the command line (optional)

Gradle needs Java 17. The simplest option is Android Studio's bundled JDK:

```sh
# macOS
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

# Windows (PowerShell)
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
```

Then, from the project root (use `gradlew.bat` instead of `./gradlew` on Windows):

```sh
./gradlew :app:assembleDebug        # build a debug APK
./gradlew :app:installDebug         # build and install on a connected device/emulator
./gradlew :app:testDebugUnitTest    # run unit tests
./gradlew :app:assembleRelease      # build a release APK
```

The APKs are written to:

- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`

You can also copy the debug APK to a phone and install it directly (allow
"Install unknown apps" when asked).

---

## 6. Feature notes

- **On-device AI (Gemini Nano)** for document extraction and summaries only
  works on supported phones (e.g. recent Pixel and Samsung flagships) where the
  model has finished downloading. On other devices and emulators those features
  are unavailable; the rest of the app works normally.
- **Push notifications** need Google Play services — use a real phone or a
  "Google Play" emulator image.
- The app talks to the backend at `https://aishield.facevitals.ai/`.

---

## 7. Troubleshooting

| Problem | Fix |
|---|---|
| `SDK location not found` | Check `sdk.dir` in `local.properties`. |
| `File google-services.json is missing` | Put the file in `app/`. |
| `Unsupported class file major version` / Java errors | Gradle must use JDK 17: Android Studio → *Settings › Build, Execution, Deployment › Build Tools › Gradle › Gradle JDK* → choose the bundled JDK 17+. |
| `JAVA_HOME is not set` (command line) | Set `JAVA_HOME` as in section 5. |
| Login / API calls fail | `TWENTYCLAUDE_APP_TOKEN` is empty or wrong in `local.properties`. Rebuild after changing it. |
| Build stuck or odd errors after pulling changes | **File › Invalidate Caches / Restart**, or run `./gradlew clean`. |
| Device not listed | Re-plug the cable, accept the USB-debugging prompt, run `adb devices`. |
