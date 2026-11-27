package com.luminousworksllc.mylingual

import android.app.Application

/**
 * Application entry point.
 *
 * Firebase itself is initialised automatically by the google-services plugin from
 * app/google-services.json. App Check has to be installed before the first Firebase AI
 * call, so it is set up here rather than in an Activity (an Activity can be recreated
 * on rotation and other lifecycle events, but the Application runs exactly once).
 *
 * The provider depends on the build type. See [installAppCheck]:
 *  - debug   (src/debug):   DebugAppCheckProviderFactory. Register the device's debug token
 *                           in Firebase console > App Check > Apps > Manage debug tokens.
 *  - release (src/release): reCAPTCHA Enterprise, so an installed APK passes App Check on any
 *                           phone. The site key comes from local.properties and is never committed.
 */
class MyLinguaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        installAppCheck()
    }
}
