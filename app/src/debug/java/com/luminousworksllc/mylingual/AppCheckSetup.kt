package com.luminousworksllc.mylingual

import android.app.Application
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/**
 * Debug builds: App Check debug provider.
 *
 * On first run, Logcat (tag DebugAppCheckProvider) prints a debug secret. Register it in
 * Firebase console > Security > App Check > Apps > (app menu) > Manage debug tokens.
 * Keep debug tokens private. Never commit one or put one in a public build.
 */
internal fun Application.installAppCheck() {
    FirebaseAppCheck.getInstance()
        .installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
    android.util.Log.e("MiLinguaAppCheck", "==========================================================")
    android.util.Log.e("MiLinguaAppCheck", "APP CHECK DEBUG MODE ACTIVE. If Gemini fails, add debug token to Firebase.")
    android.util.Log.e("MiLinguaAppCheck", "==========================================================")
}
