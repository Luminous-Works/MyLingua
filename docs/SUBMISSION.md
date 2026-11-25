# Devpost submission text: MyLingua

Draft. Edit before pasting. Every claim here is backed by code in this repository.

## Platform
Android (Kotlin, Jetpack Compose).

## Project description
MyLingua is a hands-free Spanish-English voice translator. Hold the microphone, speak, and
release. The app transcribes your speech on the device, translates it with Gemini, and reads
the natural translation aloud. The screen has one job: the words you said, and what they mean
in the other language.

## How Firebase AI Logic is used
- The app calls Gemini (`gemini-2.5-flash`) through **Firebase AI Logic** with
  `Firebase.ai(backend = GenerativeBackend.googleAI())` and `generativeModel(...)`
  (`app/src/main/java/com/luminousworksllc/mylingual/data/GeminiTranslator.kt`).
- A fixed **system instruction** turns the model into an interpreter, not a word-for-word
  translator: grammatically flawless, natural, and culturally appropriate, returning only the
  translated text.
- Each request carries the direction the user chose, so the model never has to guess the language.
- **Firebase App Check** is installed in the Application class before the first AI call
  (`MyLinguaApp.kt`), so requests come only from registered app installs.

## Features
- One screen. Press and hold to speak, release to translate. Quick tap for hands-free listening.
- Live partial transcript while you speak.
- Microphone-driven ripple animation that brightens with your voice.
- Language switch between English and Español, locked while a translation is in progress.
- Automatic spoken playback, with a voice chosen for the target language and country.
- Clear permission handling: request, retry, and a direct route to system settings if access
  was blocked.

## Value proposition
Most translation apps ask you to type, tap through menus, and read a result. MyLingua is built
for a conversation: speak, and hear the translation without looking at the screen. Using Gemini
through Firebase AI Logic means the translation reads like a person wrote it, not like a dictionary
lookup, and the speech recognition and voice output run on the device.

## Testing instructions for judges
1. Install the test build (APK link below) on an Android 7.0+ device.
2. Grant the microphone permission when prompted.
3. Hold the microphone button and say, in English, "Where is the nearest train station?"
   Release. The Spanish translation appears and is read aloud.
4. Tap **Español** to switch to Spanish input. Hold the microphone and say a Spanish sentence.
   The English translation appears and is read aloud.
5. Tap the microphone once to try hands-free listening. Tap again to finish.

**App Check note:** the test build uses App Check's debug provider. Your device must be registered
with the project's debug token before Gemini calls will succeed. The author will register it on
request; include your device's debug token from Logcat (tag `DebugAppCheckProvider`).

## Links
- Repository (Apache-2.0): https://github.com/Luminous-Works/MyLingua
- Test build (APK, v1.0.0): https://github.com/Luminous-Works/MyLingua/releases/tag/v1.0.0
- Hosted project page and direct APK download: https://mylingua.tyrone-luminousworksllc.workers.dev/
- Demo video: (add YouTube URL, under 3 minutes, English)
