package com.mgsoftware.billing.utils

import kotlinx.coroutines.delay

internal suspend fun <T> retry(
    initialDelayBeforeNextAttempt: Long = 500L,
    delayFactor: Float = 2f,
    retries: Long = 3,
    onFailedAttempt: suspend (cause: Throwable) -> Boolean = { true },
    onFailure: (suspend (cause: Throwable) -> Unit)? = null,
    block: suspend () -> T
) {
    require(initialDelayBeforeNextAttempt > 0L) { "Expected positive initial delay, but had $initialDelayBeforeNextAttempt" }
    require(delayFactor > 1f) { "Expected delay factor greater than 1, but had $delayFactor" }
    var currentDelay = initialDelayBeforeNextAttempt
    try {
        retry(
            retries = retries,
            predicate = { cause ->
                val shallRetry = onFailedAttempt(cause)
                delay(currentDelay)
                currentDelay = (currentDelay * delayFactor).toLong()
                shallRetry
            },
            block = block
        )
    } catch (cause: Throwable) {
        onFailure?.invoke(cause)
    }
}

private suspend fun <T> retry(
    retries: Long = Long.MAX_VALUE,
    predicate: suspend (cause: Throwable) -> Boolean = { true },
    block: suspend () -> T
): T {
    require(retries > 0) { "Expected positive amount of retries, but had $retries" }
    return retryWhen(
        predicate = { cause, attempt -> attempt < retries && predicate(cause) },
        block = block
    )
}

private suspend fun <T> retryWhen(
    predicate: suspend (cause: Throwable, attempt: Long) -> Boolean,
    block: suspend () -> T
): T {
    var attempt = 0L
    try {
        attempt++
        return block()
    } catch (cause: Throwable) {
        var shallRetry = false
        while (true) {
            try {
                if (predicate(cause, attempt)) {
                    shallRetry = true
                    attempt++
                    return block()
                } else {
                    shallRetry = false
                    throw cause
                }
            } catch (cause: Throwable) {
                if (!shallRetry) {
                    throw cause
                }
            }
        }
    }
}