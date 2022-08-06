package com.example.app.ui.common.view

import android.content.Context
import android.view.View
import androidx.annotation.IdRes

interface IView {

    fun getRootView(): View

    fun <T : View> findViewById(@IdRes resId: Int): T = getRootView().findViewById(resId)

    fun getContext(): Context = getRootView().context
}