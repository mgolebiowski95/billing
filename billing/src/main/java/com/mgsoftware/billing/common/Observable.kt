package com.mgsoftware.billing.common

interface Observable<T> {

    fun registerListener(listener: T)

    fun unregisterListener(listener: T)
}