package com.example.app.ui.common.view

interface IObservableView<T> : IView {

    fun registerListener(listener: T)

    fun unregisterListener(listener: T)
}