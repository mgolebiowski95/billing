package com.example.app.ui.common.view

abstract class BaseObservableView<T> : BaseView(), IObservableView<T> {

    private val listeners = mutableSetOf<T>()

    override fun registerListener(listener: T) {
        listeners.add(listener)
    }

    override fun unregisterListener(listener: T) {
        listeners.remove(listener)
    }

    fun getListeners(): Set<T> = listeners
}