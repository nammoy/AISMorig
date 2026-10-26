package com.app.aisheildme.bytrix

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import com.app.aisheildme.bytrix.core.designsystem.TwentyClaudeTheme
import com.app.aisheildme.bytrix.navigation.AppNavHost
import com.app.aisheildme.bytrix.core.common.DetailHandoff
import com.app.aisheildme.bytrix.core.util.FileOpener
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var detailHandoff: DetailHandoff

    @Inject lateinit var fileOpener: FileOpener

    private val requestNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {  }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            TwentyClaudeTheme {
                AppNavHost(handoff = detailHandoff, fileOpener = fileOpener)
            }
        }
    }
}
