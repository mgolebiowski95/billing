package com.mgsoftware.billing.common

abstract class BaseObservable<T> : Observable<T> {
    private val listeners = mutableSetOf<T>()

    override fun registerListener(listener: T) {
        listeners.add(listener)
    }

    override fun unregisterListener(listener: T) {
        listeners.remove(listener)
    }

    fun getListeners(): Set<T> = listeners
}