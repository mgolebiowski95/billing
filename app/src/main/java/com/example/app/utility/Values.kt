package com.example.app.utility

interface Values {

    fun getString(stringResourceId: Int): String

    fun getString(stringResourceId: Int, formatArgs: Array<Any>): String

    fun getColor(colorResourceId: Int): Int

    fun getDimension(value: Float): Float

    fun getInteger(integerResourceId: Int): Int

    fun getLong(integerResourceId: Int): Long

    fun getIdentifier(key: String, type: String): Int

    fun getShortAnimTime(): Long

    fun getMediumAnimTime(): Long

    fun getLongAnimTime(): Long
}