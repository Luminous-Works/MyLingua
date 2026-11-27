# Devpost submission text: MiLingua

Paste into Devpost. Fill in the items marked TODO first.

## Platform
Android (Kotlin, Jetpack Compose).

## Inspiration
Most translation apps are built around typing, menus and reading. In a real conversation you want
to speak and hear the answer.

## What it does
- One screen for English and Spanish, in either direction.
- Hold the microphone to speak and release to translate, or tap for hands-free listening.
- Shows a live partial transcript, then the translation, and reads it aloud.
- Shows whether the phone or the cloud answered.
- A Caribbean-inspired design with a water-ripple animation that responds to your voice.
- Four themes: Night Ocean, Caribbean Sand, Sunny Day and Overcast.

## How we used Firebase AI Logic
- **Hybrid inference:** `Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(...)`
  with `OnDeviceConfig(mode = InferenceMode.PREFER_ON_DEVICE, modelOption = OnDeviceModelOption.PREVIEW)`,
  the on-device category used for Gemini Nano 4. The app prefers the phone's model and otherwise
  uses `gemini-3.5-flash-lite` in the cloud. Code: `MainActivity.kt`.
- **Interpreter system instruction:** asks for natural, culturally appropriate output with no commentary.
- **App Check:** installed in the `Application` class before the first AI call. The released test
  build uses the reCAPTCHA provider; debug builds use the debug provider.

**Test status:** our test phone (DOOGEE Note 58) reports the on-device model as unavailable, so its
translations are answered by the cloud. The on-device path is integrated and builds, but we have
not run Gemini Nano on a supported device.

## Value proposition
A focused interpreter for face-to-face English-Spanish conversation: speak, then hear the
translation. The hybrid setup is designed to use the phone's own model where one is available and
use the cloud on phones without it.

## Challenges
- A fallback path in the code hid the real error behind a 404 from a retired model. Removing that
  path showed the cause: an unregistered App Check debug token.
- A cloud model reported high demand, so we moved to Flash-Lite and added a 20-second timeout per try.
- Firebase's on-device library required Kotlin 2.4 and Android 8.0 or later.

## What's next
Adding Haitian Creole, to connect English, Spanish and Kreyòl speakers across Jamaica, Cuba and Haiti,
which share a long history. Also: testing Gemini Nano on supported hardware, and Play Integrity for a
Play Store release.

## What we built during the submission period
The whole app was built during the submission period; work started on Nov 25, 2026, with no earlier
version. That covers voice capture, translation through Firebase AI Logic, spoken output, App Check,
hybrid on-device/cloud inference and the interface.

AI coding assistants used: Gemini and Claude Code.

## Testing instructions
1. Install the test build on an Android 8.0 or later phone.
2. Allow microphone access.
3. Hold the microphone, say "Where is the nearest train station?" and release. The Spanish
   translation appears and is read aloud.
4. Tap **Español**, then say a Spanish sentence to hear it in English.

No App Check debug token is needed: the test build uses the reCAPTCHA provider. It has been tested on
one phone. An internet connection is required for cloud translation.

## Links
- Repository (Apache-2.0): https://github.com/Luminous-Works/MyLingua
- Test build: https://github.com/Luminous-Works/MyLingua/releases/tag/v1.2.1
- Hosted project page: TODO. Update it before listing it.
- Demo video: TODO (public YouTube, 3 minutes or less, English)
