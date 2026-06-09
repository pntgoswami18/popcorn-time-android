package com.popcorntime.android

import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.popcorntime.android.data.preferences.ThemeMode
import com.popcorntime.android.data.preferences.ThemePrefsStore
import com.popcorntime.android.ui.main.MainScreen
import com.popcorntime.android.ui.theme.PopcornTimeTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var themePrefsStore: ThemePrefsStore

    private var _isPlayerVisible = false

    fun setPlayerVisible(visible: Boolean) {
        _isPlayerVisible = visible
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (_isPlayerVisible && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        try {
            com.google.android.gms.cast.framework.CastContext.getSharedInstance(this)
        } catch (e: Exception) {
            // Cast not available on this device (no Google Play Services)
        }
        val isTv = packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
        setContent {
            val themeMode by themePrefsStore.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            PopcornTimeTheme(themeMode = themeMode) {
                val systemUiController = rememberSystemUiController()
                systemUiController.setSystemBarsColor(
                    color = androidx.compose.ui.graphics.Color.Transparent,
                    darkIcons = false,
                )
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(isTv = isTv)
                }
            }
        }
    }
}
