package com.luminousworksllc.mylingual

import android.app.Application
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/**
 * Application entry point.
 *
 * Firebase itself is initialised automatically by the google-services plugin from
 * app/google-services.json. App Check has to be installed before the first Firebase AI
 * call, so it is set up here rather than in an Activity (an Activity can be recreated
 * on rotation and other lifecycle events, but the Application runs exactly once).
 *
 * DEBUG PROVIDER ONLY. The debug provider is for development. On first run, Logcat prints
 * a debug secret ("Enter this debug secret into the allow list..."). Register it in
 * Firebase console > App Check > Apps > your app > Manage debug tokens.
 *
 * Before a Play Store release, switch to PlayIntegrityAppCheckProviderFactory (add the
 * com.google.firebase:firebase-appcheck-playintegrity dependency). Otherwise builds
 * installed from Play will be rejected by App Check.
 */
class MyLinguaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseAppCheck.getInstance()
            .installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
    }
}
