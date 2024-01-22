package com.flixclusive.crash.di

import com.flixclusive.crash.CrashReportSender
import com.flixclusive.crash.DefaultCrashReportSender
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class CrashReportSenderModule {
    @Binds
    abstract fun providesCrashReportSender(
        defaultCrashReportSender: DefaultCrashReportSender,
    ): CrashReportSender
}