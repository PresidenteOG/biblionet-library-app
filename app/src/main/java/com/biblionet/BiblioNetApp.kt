package com.biblionet

import android.app.Application
import android.content.pm.ApplicationInfo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Application entry point. Its only job beyond the Android default is to point
 * Firebase Auth and Firestore at the local emulator suite on debug builds — see
 * [EMULATOR_ENABLED]. Runs before any Activity or ViewModel touches Firebase, so
 * every later `FirebaseAuth.getInstance()` / `FirebaseFirestore.getInstance()`
 * call site is redirected without needing its own changes.
 */
class BiblioNetApp : Application() {

    companion object {
        /**
         * Local Firebase Emulator Suite switch. Debug builds only — a release
         * build's [ApplicationInfo.FLAG_DEBUGGABLE] is always false, so a signed
         * APK never talks to the emulator even if this stays true.
         *
         * Flip to `false` to test a debug build against the real Firebase
         * project instead. Requires `firebase emulators:start` running first
         * (see scripts/seed-emulator/seed.js to populate demo data) and, on an
         * Android emulator (not a physical device), `10.0.2.2` to reach the
         * host machine's localhost.
         */
        private const val EMULATOR_ENABLED = true
        private const val EMULATOR_HOST = "10.0.2.2"
        private const val AUTH_EMULATOR_PORT = 9099
        private const val FIRESTORE_EMULATOR_PORT = 8080
    }

    override fun onCreate() {
        super.onCreate()

        val isDebugBuild = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (isDebugBuild && EMULATOR_ENABLED) {
            FirebaseAuth.getInstance().useEmulator(EMULATOR_HOST, AUTH_EMULATOR_PORT)
            FirebaseFirestore.getInstance().useEmulator(EMULATOR_HOST, FIRESTORE_EMULATOR_PORT)
        }
    }
}
