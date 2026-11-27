package com.luminousworksllc.mylingual

import android.app.Application
import android.util.Log
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory

/**
 * Release builds: App Check with the reCAPTCHA Enterprise provider (Firebase documents this
 * provider as Preview on Android). It works for APKs installed from outside Google Play, so
 * judges can install the GitHub release on any phone.
 *
 * Setup: create an Android-type reCAPTCHA key for package com.luminousworksllc.mylingual in
 * Google Cloud, register it under Firebase console > App Check > Apps, and put
 * `recaptcha.siteKey=...` in local.properties before building.
 */
internal fun Application.installAppCheck() {
    val siteKey = BuildConfig.RECAPTCHA_SITE_KEY
    if (siteKey.isBlank()) {
        // Fail loudly in Logcat. With enforcement on, Gemini calls will be rejected.
        Log.e("MyLingua", "No reCAPTCHA site key: set recaptcha.siteKey in local.properties")
        return
    }
    FirebaseAppCheck.getInstance()
        .installAppCheckProviderFactory(RecaptchaAppCheckProviderFactory.getInstance(siteKey))
}
