# MiLingua

A Spanish-English voice interpreter for Android. Hold the microphone, speak, release.
MiLingua transcribes what you said, translates it through Firebase AI Logic, and reads the
translation aloud.

Built for the #BuildHybridAI 2026 hackathon. Licensed under the [Apache License 2.0](LICENSE).

## Why

Most translation apps are built around typing and reading. A conversation needs you to speak
and hear the answer. MiLingua is one screen made for that.

## How it works

| Step | Component |
|---|---|
| Listen | Android `SpeechRecognizer` (the phone's speech service), with live partial results |
| Translate | Firebase AI Logic hybrid inference: on-device Gemini Nano when available, otherwise `gemini-3.5-flash-lite` in the cloud |
| Speak | Android `TextToSpeech`, set to `es-ES` or `en-US` |
| Protect | Firebase App Check, installed before the first AI call |

All of this is in `app/src/main/java/com/luminousworksllc/mylingual/MainActivity.kt`, apart from
App Check setup (`MyLinguaApp.kt` and the per-build-type `AppCheckSetup.kt`).

### Hybrid inference

The model is created with
`OnDeviceConfig(mode = InferenceMode.PREFER_ON_DEVICE, modelOption = OnDeviceModelOption.PREVIEW)`.
`PREVIEW` is the on-device model category used for Gemini Nano 4. At startup the app checks the
on-device model status and requests a download if the model is downloadable. A label under each
translation shows which one answered: `ON-DEVICE · GEMINI NANO` or `CLOUD · GEMINI`.

The system instruction is:

> You are MiLingua, a highly skilled interpreter. Translate the input between
> English and Spanish. Ensure the output is grammatically flawless, natural-sounding, and
> culturally appropriate. Only return the translated text itself, with no extra commentary.

## Test status

| What | Result |
|---|---|
| Debug build on a DOOGEE Note 58 (Android 16): translation and spoken output | Works, answered by the **cloud** |
| On-device model status on that phone | `UNAVAILABLE` (no AICore package found on the phone) |
| **On-device inference with Gemini Nano** | **Not tested on any device**, including Spanish output quality |
| On-device model download | Not tested |
| Release build | Not tested |

Firebase's on-device SDK is experimental. Check its current documentation for supported devices
and languages.

## Use the app

- **Hold** the microphone to speak. Release to translate.
- **Tap** the microphone for hands-free listening. Tap again to finish.
- Tap **English** or **Español** to choose the language you speak. The other one is the output.
- Allow microphone access when asked.
- The cloud path needs an internet connection. Offline use has not been tested.

## Build and run

Requirements: a current Android Studio, a device or emulator with Android 8.0 (API 26) or later,
and a Firebase project.

1. Create a Firebase project at <https://console.firebase.google.com>.
2. Open **AI Logic** and enable the **Gemini Developer API**.
3. Register an Android app with package name `com.luminousworksllc.mylingual`.
4. Download `google-services.json` to `app/google-services.json`. It is git-ignored. Never commit it.
5. Open the project in Android Studio and run the `app` configuration (debug).
6. **App Check:** in Logcat, find the debug token printed by `DebugAppCheckProvider`. Add it in the
   Firebase console under App Check > Apps > Manage debug tokens. Until then, cloud calls are
   rejected. Keep the token private.

Release builds use the reCAPTCHA App Check provider and need `recaptcha.siteKey` in
`local.properties`. That path has not been tested.

## Repository note

`data/`, `model/`, `speech/` and `ui/translate/` contain an earlier version of the app. The current
screen in `MainActivity.kt` does not use its translator, speech or screen classes.
