package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.ui.FtpViewModel
import com.example.ui.MainServerScreen
import com.example.ui.components.SciFiSplashScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: FtpViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val darkThemeOverride by viewModel.isDarkThemeOverride.collectAsState()
            val isDarkTheme = darkThemeOverride ?: isSystemInDarkTheme()
            var showSplash by rememberSaveable { mutableStateOf(true) }

            MyApplicationTheme(darkTheme = if (showSplash) true else isDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Crossfade(
                        targetState = showSplash,
                        animationSpec = tween(400),
                        label = "splash_crossfade"
                    ) { isSplashScreen ->
                        if (isSplashScreen) {
                            SciFiSplashScreen(
                                onComplete = { showSplash = false }
                            )
                        } else {
                            MainServerScreen(
                                viewModel = viewModel,
                                isDarkTheme = isDarkTheme,
                                onToggleDarkTheme = { isDark -> viewModel.toggleTheme(isDark) }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkPermissions()
        viewModel.refreshNetwork()
    }
}
