package com.flixclusive.core.testing.extensions

import com.flixclusive.core.network.util.Resource
import strikt.api.Assertion.Builder
import strikt.assertions.isA

/**
 * An extension function to assert that a [Resource] is of type [Resource.Success].
 * */
fun <T> Builder<Resource<T>>.isSuccess(): Builder<Resource.Success<T>> =
    isA<Resource.Success<T>>()

/**
 * An extension function to assert that a [Resource] is of type [Resource.Failure].
 * */
fun <T> Builder<Resource<T>>.isFailure(): Builder<Resource.Failure> =
    isA<Resource.Failure>()
