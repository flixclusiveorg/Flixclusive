package com.flixclusive.crash

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.theme.util.TvModeChecker.isTvMode
import com.flixclusive.feature.mobile.crash.CrashMobileScreen
import com.flixclusive.mobile.MobileActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
internal class CrashActivity : ComponentActivity() {

    @Inject
    lateinit var reportSender: CrashReportSender

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val errorMessage = intent?.extras?.getString(ERROR_MESSAGE)
        val softwareInfo = intent?.extras?.getString(SOFTWARE_INFO)

        val isTv = isTvMode()

        setContent {
            FlixclusiveTheme(isTv = isTv) {
                // if(!isTv) {
                    CrashMobileScreen(
                        softwareInfo = softwareInfo,
                        errorMessage = errorMessage,
                        onDismiss = {
                            reportSender.send(
                                errorLog = "$softwareInfo\n=======\n$errorMessage"
                            )

                            finishAffinity()
                            startActivity(
                                Intent(this, MobileActivity::class.java)
                            )
                        }
                    )
                // }
            }
        }
    }
}