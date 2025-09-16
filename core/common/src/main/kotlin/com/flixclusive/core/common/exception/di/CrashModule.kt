package com.flixclusive.core.common.exception.di

import com.flixclusive.core.common.exception.CrashReportSender
import com.flixclusive.core.common.exception.CrashReportSenderImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class CrashModule {
    @Singleton
    @Binds
    abstract fun providesCrashReportSender(
        crashReportSenderImpl: CrashReportSenderImpl,
    ): CrashReportSender
}
