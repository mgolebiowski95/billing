package com.mgsoftware.billing

interface SkuProvider {

    fun getAllSkus(): Set<String>

    fun getSkusByType(types: Set<Type>): Set<String>

    fun geInappSkus(): Set<String> {
        return getSkusByType(setOf(Type.INAPP))
    }

    fun geSubsSkus(): Set<String> {
        return getSkusByType(setOf(Type.SUBS))
    }

    fun getConsumableSkus(): Set<String> {
        return getSkusByType(setOf(Type.CONSUMABLE))
    }

    fun getNonConsumableSkus(): Set<String> {
        return getSkusByType(setOf(Type.NON_CONSUMABLE))
    }

    data class SkuDetails(
        val sku: String,
        val types: Set<Type>
    )

    enum class Type {
        INAPP,
        SUBS,
        CONSUMABLE,
        NON_CONSUMABLE
    }
}