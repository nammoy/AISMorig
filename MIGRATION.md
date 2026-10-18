# Flutter → Kotlin/Android-native migration

The Flutter app (`../lib`, ~27.8k lines of Dart) has been ported to a native
Kotlin/Compose app. **Android only — iOS is dropped.**

Status: **complete**. All 29 live routes, 15 feature modules and 17 services
have Kotlin counterparts. Debug and release both build; 50 unit tests pass.

The Flutter source is still in the repo as the porting reference. Once you have
run the native app against staging and are satisfied, delete `../lib`,
`../ios`, `../pubspec.yaml`, `../pubspec.lock` and `../android`.

---

## Stack mapping

| Flutter | Kotlin/native |
|---|---|
| GetX `GetxController` | `ViewModel` + `StateFlow` |
| `Obx(() => …)` / `.obs` | `collectAsStateWithLifecycle()` |
| `Get.toNamed` / `offNamed` / `offAllNamed` | Navigation-Compose + `navigateReplacing` / `navigateAndClear` |
| `Get.arguments` (untyped) | Type-safe `@Serializable` routes; `DetailHandoff` for whole objects |
| `Get.bottomSheet` | `ModalBottomSheet` |
| `Get.dialog` | `Dialog` / `AlertDialog` rendered from state |
| `Get.lazyPut` bindings | Hilt (`hiltViewModel()`, scoped to the back-stack entry) |
| dio + interceptors | Retrofit + OkHttp interceptors |
| `infinite_scroll_pagination` | `PagedState<T>` + `PagedList` composable |
| `get_storage` / `shared_preferences` | DataStore Preferences |
| `sqflite` | Room (same DB filename and schema) |
| Flutter widgets | Jetpack Compose + Material 3 |
| `CustomToast` (global context) | `UiMessage` in state + snackbar host |
| `flutter_screenutil` (`.w`/`.h`/`.sp`) | `dp`/`sp` — Compose scales natively |
| `image_picker` | Photo Picker (`PickVisualMedia`) |
| `file_picker` | `ActivityResultContracts.OpenDocument` |
| `open_file` + `dio.download` | OkHttp + `FileProvider` + `ACTION_VIEW` |
| `share_plus` | `ACTION_SEND` + `FileProvider` |
| `signature` | Custom Compose `SignaturePad` → PNG |
| `pdf` (generation) | `android.graphics.pdf.PdfDocument` |
| `syncfusion_flutter_pdfviewer` | Handed to the device's PDF viewer by intent |
| `flutter_html` | `WebView` with an injected stylesheet |
| `local_auth` | `androidx.biometric` |
| `firebase_messaging` + `flutter_local_notifications` | Firebase Messaging + `NotificationCompat` |
| `google_mlkit_*` | ML Kit native SDKs |
| `device_info_plus` | `android.os.Build` |

---

## What was ported

**Foundation** — Gradle with version catalog (AGP 8.11.1, Kotlin 2.2.0,
Compose BOM, Hilt, KSP, Room), design system (colour tokens, typography,
Material 3 theme), 20+ shared components, Retrofit + three interceptors, all
~45 endpoints, DataStore session, Room database, navigation graph.

**Auth** — Splash · Onboarding · Login · Signup · Basic Info · Verify Email
(OTP) · Setup Profile · Create Vault PIN · Reset/Change Password.

**Main app** — Dashboard shell (bottom nav + centre FAB) · Home · Folders ·
Documents (upload/open/share/delete) · Share · Notifications · Healthcare ·
Appointment detail · Retail + receipt · Banking + statement · Check-in history ·
Add invoice · Profile · Edit profile · Reset PIN · Availability · Privacy policy.

**Patient check-in** — the form (35+ fields, prefill, scan-to-fill), ML Kit
document scanner + OCR, Gemini Nano extraction and summarisation, signature
capture, preview, submission, clinical review, and two PDF generators.

**Vault gate** — PIN + biometric unlock with the `pin_expiry_time` window.

### Not ported, deliberately

- **`lib/screens/document/`** — an unbuilt stub in Dart too
  (`// TODO: Build out the document feature`). Nothing to carry over.
- **`forgotPassword`, `welcome`, `home`, `profile` routes** — declared in
  `app_routes.dart` but never registered in `app_pages.dart`. `home` and
  `profile` are dashboard tabs, not destinations.
- **Social sign-in** — the Google/Apple path was already commented out in
  `authentication_service.dart`.

---

## Behaviours preserved deliberately

Each has a comment at the site explaining why:

- The `api_token` is sent **verbatim** — already base64-encoded by the backend,
  no `Bearer` prefix. Re-encoding gives a 401.
- **428 on login** means "email not verified": redirect to OTP, don't show an error.
- Profile GET/PATCH **don't echo** `api_token`, `is_pin_set`, `is_email_verify` —
  they must be merged from the existing session or a refresh drops the token.
- Insurance / family doctor / driver licence read short keys **only** from a
  nested object — a flat fallback collides with the user's own `name`,
  `address` and `state`. Pinned by tests.
- The backend spells the biometric flag **`is_enable_biomatric`**.
- The vault-unlock window is **in-memory only**, so a restart re-prompts.
- Gemini Nano: `DOWNLOADABLE` is **not** ready — the download must complete
  first, and both FULL and FAST variants are probed.
- Scanned values **overwrite** the profile prefill (the paper in hand is newer);
  prefill only **seeds empty** fields (never clobbers typing).
- A short page ends a paginated list (`items.size < pageSize`).
- Free-text vitals parsing: feet/inches, pounds, and the `—` / "None on file"
  sentinels. Pinned by tests.

## Deliberate changes

- **Secrets** moved out of source into `local.properties`. They were committed
  in `api_constants.dart`; **they remain in this repo's git history**, so
  rotate the AI-ShieldMe app token if it is sensitive. (Stripe was removed — the app has no payments.)
- **Gemini Nano** now calls ML Kit directly — `OnDeviceGenAi.kt` was already
  Kotlin, so only its `MethodChannel` wrapper was removed.
- **The 401 handler** clears the session instead of navigating; the nav host
  observes that and routes to login. Same outcome, no global navigator.
- **Downloads** go through `MediaStore` (scoped storage), not a raw path.
- **`Unit` tests** cover the parsing rules the Dart code encoded defensively.

---

## Known gaps before shipping

1. **Not run on a device.** Verification is compile + unit tests only.
2. **Release signing** still uses the debug keystore — see `app/build.gradle.kts`.
3. **Assets not copied.** Splash and onboarding draw placeholder blocks where
   `app_logo.png` and `onb1–3.png` go. Launcher icons *were* carried over.
4. **Gilroy font** — never in the repo; the Flutter app silently fell back to
   the platform default and so does this. Drop the `.ttf` files into
   `res/font` and point `AppFontFamily` at them.
5. **No instrumentation or Compose UI tests** yet.

---

## Build

Android Studio provides the JDK; there is no `java` on the PATH:

```sh
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleRelease
```

Before the first build, copy `local.properties.example` to `local.properties`
and fill in the token values. `google-services.json` is git-ignored — copy it
from the Firebase console (project `ai-sheildme-dev`).

Note: `minSdk 26` is required by AICore/Gemini Nano, and the debug build has
**no** `applicationIdSuffix` because `google-services.json` registers only
`com.app.aisheildme.bytrix`.
