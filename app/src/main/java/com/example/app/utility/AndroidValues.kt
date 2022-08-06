package com.example.app.utility

import android.content.Context
import android.util.TypedValue
import androidx.core.content.ContextCompat

class AndroidValues(
    private val context: Context
) : Values {

    override fun getString(stringResourceId: Int): String {
        return context.getString(stringResourceId)
    }

    override fun getString(stringResourceId: Int, formatArgs: Array<Any>): String {
        return context.getString(stringResourceId, *formatArgs)
    }

    override fun getColor(colorResourceId: Int): Int {
        return ContextCompat.getColor(context, colorResourceId)
    }

    override fun getDimension(value: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value,
            context.resources.displayMetrics
        )
    }

    override fun getInteger(integerResourceId: Int): Int {
        return context.resources.getInteger(integerResourceId)
    }

    override fun getLong(integerResourceId: Int): Long {
        return context.resources.getInteger(integerResourceId).toLong()
    }

    override fun getIdentifier(key: String, type: String): Int {
        return context.resources.getIdentifier(key, type, context.packageName)
    }

    override fun getShortAnimTime(): Long {
        return getLong(android.R.integer.config_shortAnimTime)
    }

    override fun getMediumAnimTime(): Long {
        return getLong(android.R.integer.config_mediumAnimTime)
    }

    override fun getLongAnimTime(): Long {
        return getLong(android.R.integer.config_longAnimTime)
    }
}