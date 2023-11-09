package com.mgsoftware.billing

interface ProductIdProvider {

    fun getAllProductIds(): Set<String>

    fun getProductIdsByType(types: Set<Type>): Set<String>

    fun geInappProductIds(): Set<String> {
        return getProductIdsByType(setOf(Type.INAPP))
    }

    fun geSubsProductIds(): Set<String> {
        return getProductIdsByType(setOf(Type.SUBS))
    }

    fun getConsumableProductIds(): Set<String> {
        return getProductIdsByType(setOf(Type.CONSUMABLE))
    }

    fun getNonConsumableProductIds(): Set<String> {
        return getProductIdsByType(setOf(Type.NON_CONSUMABLE))
    }

    data class ProductDetails(
        val id: String,
        val types: Set<Type>
    )

    enum class Type {
        INAPP,
        SUBS,
        CONSUMABLE,
        NON_CONSUMABLE
    }
}