package com.example.app.billing

import com.mgsoftware.billing.ProductIdProvider

class AppProductIdProvider : ProductIdProvider {
    private val data = mutableSetOf<ProductIdProvider.ProductDetails>()

    init {

        // subs
        data.add(
            ProductIdProvider.ProductDetails(
                GOLD_MONTHLY, setOf(
                    ProductIdProvider.Type.SUBS, ProductIdProvider.Type.NON_CONSUMABLE
                )
            )
        )

        // in_app
        data.add(
            ProductIdProvider.ProductDetails(
                PREMIUM_CAR, setOf(
                    ProductIdProvider.Type.INAPP, ProductIdProvider.Type.NON_CONSUMABLE
                )
            )
        )
        data.add(
            ProductIdProvider.ProductDetails(
                GAS, setOf(
                    ProductIdProvider.Type.INAPP, ProductIdProvider.Type.CONSUMABLE
                )
            )
        )
    }

    override fun getAllProductIds(): Set<String> {
        return data.mapTo(mutableSetOf()) { it.id }
    }

    override fun getProductIdsByType(types: Set<ProductIdProvider.Type>): Set<String> {
        return data.filter { it.types.intersect(types).isNotEmpty() }
            .mapTo(mutableSetOf()) { it.id }
    }

    companion object {
        const val GOLD_MONTHLY = "gold_monthly"
        const val PREMIUM_CAR = "premium_car"
        const val GAS = "gas"
    }
}