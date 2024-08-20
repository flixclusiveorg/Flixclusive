package com.flixclusive.domain.provider.test

data class AssertionResult<T>(
    val status: TestStatus,
    val data: T? = null,
    val error: Throwable? = null,
)