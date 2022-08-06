package com.example.app.ui.common.view

import android.view.View

abstract class BaseView : IView {

    lateinit var root: View

    override fun getRootView(): View {
        return root
    }

    fun setRootView(view: View) {
        root = view
    }
}