package com.flixclusive.service

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ServiceTestRule
import com.flixclusive.service.app_updater.AppUpdaterService
import com.flixclusive.service.app_updater.EXTRA_UPDATE_URL
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.TimeoutException


@RunWith(AndroidJUnit4::class)
class AppUpdaterServiceTest {
    @get:Rule
    val serviceRule = ServiceTestRule()

    @Test
    @Throws(TimeoutException::class)
    fun testAppUpdaterService() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val url =
            "https://github.com/rhenwinch/Flixclusive/releases/latest/download/flixclusive-v1.2.1-release.apk"

        val serviceIntent = Intent(
            context,
            AppUpdaterService::class.java
        ).apply {
            putExtra(EXTRA_UPDATE_URL, url)
        }

        serviceRule.startService(serviceIntent)
        Thread.sleep(10000)
        assert(File(context.externalCacheDir, "update.apk").exists())
    }
}