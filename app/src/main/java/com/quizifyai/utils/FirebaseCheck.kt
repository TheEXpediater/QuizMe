package com.quizifyai.utils

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

object FirebaseCheck {
    private const val TAG = "FirebaseCheck"

    fun initialize(context: Context): Boolean {
        return try {
            val app = (if (FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseApp.getInstance()
            } else {
                FirebaseApp.initializeApp(context)
            }) ?: throw IllegalStateException("FirebaseApp failed to initialize")

            val auth = FirebaseAuth.getInstance(app)
            val firestore = FirebaseFirestore.getInstance(app)
            val storage = FirebaseStorage.getInstance(app)

            Log.d(TAG, "Firebase initialized successfully")
            Log.d(TAG, "Auth instance ready: ${auth.currentUser != null}")
            Log.d(TAG, "Firestore instance ready: ${firestore.app.name}")
            Log.d(TAG, "Storage instance ready: ${storage.app.name}")

            true
        } catch (e: Exception) {
            Log.e(TAG, "Firebase initialization failed", e)
            false
        }
    }

    fun testFirestoreReadiness(): Boolean {
        return try {
            val firestore = FirebaseFirestore.getInstance()
            Log.d(TAG, "Firestore ready: ${firestore.app.name}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Firestore readiness check failed", e)
            false
        }
    }

    fun testStorageReadiness(): Boolean {
        return try {
            val storage = FirebaseStorage.getInstance()
            Log.d(TAG, "Storage ready: ${storage.app.name}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Storage readiness check failed", e)
            false
        }
    }

    fun testAuthReadiness(): Boolean {
        return try {
            val auth = FirebaseAuth.getInstance()
            Log.d(TAG, "Auth ready: ${auth.app.name}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Auth readiness check failed", e)
            false
        }
    }
}
