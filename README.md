# MiLingua

A distraction-free, real-time Spanish-English voice translator for Android.
Hold the microphone, speak, release. MiLingua transcribes you on-device, translates
with Gemini through Firebase AI Logic, and speaks the natural translation aloud.

Built for the #BuildHybridAI Hackathon. Licensed under the [Apache License 2.0](LICENSE).

## How it works

| Step | Component | Where |
|---|---|---|
| 1. Listen | Android `SpeechRecognizer` (on-device, streams partial results) | `speech/SpeechTranscriber.kt` |
| 2. Translate | Gemini `gemini-1.5-flash` via Firebase AI Logic, with an interpreter system instruction | `data/GeminiTranslator.kt` |
| 3. Speak | Android `TextToSpeech`, voice chosen by country match, then quality | `speech/VoiceSpeaker.kt` |
| State | Sealed `Phase`: Idle, Listening, Translating, Speaking | `ui/translate/TranslateViewModel.kt` |
| Security | Firebase App Check, installed before the first AI call | `MyLinguaApp.kt` |

The system instruction sent to Gemini is:

> You are MiLingua, a highly skilled interpreter. Translate the input between
> English and Spanish. Ensure the output is grammatically flawless, natural-sounding, and
> culturally appropriate. Only return the translated text itself, with no extra commentary.

## Use the app

- **Hold** the microphone to speak. Release to translate.
- **Tap** the microphone to start hands-free listening. Tap again to finish.
- Tap **English** or **Español** to choose the language you speak. The other one is the output.
- Grant microphone access when asked. If you choose "Don't allow" twice, use "Open settings".

## Build and run (for developers)

Requirements: Android Studio (current stable), JDK 17+, an Android device or emulator running
Android 7.0 (API 24) or later, and a Firebase project.

1. **Create a Firebase project** at <https://console.firebase.google.com>.
2. **Enable Firebase AI Logic:** in the console, open AI Logic and enable the **Gemini Developer API**.
3. **Register the Android app** with package name `com.luminousworksllc.mylingual`
   (or change the `applicationId` and `namespace` in `app/build.gradle.kts` to your own).
4. **Download `google-services.json`** from the console and place it at `app/google-services.json`.
   This file is git-ignored on purpose. Never commit it.
5. **Set up App Check (debug):** run the app once. Logcat prints a debug secret from
   `DebugAppCheckProvider`. Add it in the console under App Check > Apps > Manage debug tokens.
   Without this, Gemini calls are rejected.
6. Open the project in Android Studio and run the `app` configuration.

## Repository layout

```
app/src/main/java/com/luminousworksllc/mylingual/
  MainActivity.kt              single Activity, hosts the Compose screen & MiLinguaViewModel
  MyLinguaApp.kt               Application: App Check setup
  model/Language.kt            English and Spanish, with speech tags and TTS locales
  data/GeminiTranslator.kt     Firebase AI Logic call and system instruction
  speech/SpeechTranscriber.kt  SpeechRecognizer wrapper
  speech/VoiceSpeaker.kt       TextToSpeech wrapper
```

## Status

Compiles with `app:compileDebugKotlin`. Runtime behaviour on device is being tested.
