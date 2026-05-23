package com.quizifyai

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import androidx.lifecycle.lifecycleScope
import com.quizifyai.presentation.components.QuizifyTheme
import com.quizifyai.presentation.navigation.QuizifyNavHost
import com.quizifyai.utils.AppContainer
import com.quizifyai.utils.FirebaseCheck
import com.quizifyai.utils.GeminiCheck
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            val firebaseReady = FirebaseCheck.initialize(this@MainActivity)
            Log.d("MainActivity", "Firebase initialization result: $firebaseReady")

            val geminiReady = GeminiCheck.runCheck()
            Log.d("MainActivity", "Gemini test result: $geminiReady")
        }

        setContent {
            val appContainer = remember { AppContainer(applicationContext) }
            QuizifyTheme {
                QuizifyNavHost(appContainer)
            }
        }
    }
}
