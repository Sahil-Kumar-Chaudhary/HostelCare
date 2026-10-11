package com.hostelcare.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.hostelcare.app.ui.navigation.HostelCareAppNavHost
import com.hostelcare.app.ui.theme.HostelCareTheme
import com.hostelcare.app.ui.theme.ThemeManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.init(this)
        val app = application as HostelCareApp
        setContent {
            val isDarkMode by ThemeManager.isDarkMode.collectAsStateWithLifecycle()
            HostelCareTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize().systemBarsPadding().imePadding(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    HostelCareAppNavHost(navController = navController, appContainer = app)
                }
            }
        }
    }
}
