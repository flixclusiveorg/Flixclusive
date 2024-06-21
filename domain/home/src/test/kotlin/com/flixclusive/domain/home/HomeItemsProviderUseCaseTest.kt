package com.flixclusive.domain.home

import com.flixclusive.core.util.log.LogRule
import com.flixclusive.domain.home.di.TestHomeDomainModule.getMockHomeItemsProviderUseCase
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class HomeItemsProviderUseCaseTest {
    @get:Rule
    val rule = LogRule()

    private val scope = TestScope(UnconfinedTestDispatcher())

    private lateinit var homeItemsProviderUseCase: HomeItemsProviderUseCase

    @Before
    fun setUp() {
        homeItemsProviderUseCase = getMockHomeItemsProviderUseCase(scope = scope)
    }

    @Test
    fun `Get home items`() = scope.runTest {
        homeItemsProviderUseCase()

        println(homeItemsProviderUseCase._rowItems.value)
    }
}