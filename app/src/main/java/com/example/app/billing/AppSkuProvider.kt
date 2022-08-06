package com.example.app.billing

import com.mgsoftware.billing.SkuProvider

class AppSkuProvider : SkuProvider {
    private val data = mutableSetOf<SkuProvider.SkuDetails>()

    init {

        // subs
        data.add(
            SkuProvider.SkuDetails(
                GOLD_MONTHLY, setOf(
                    SkuProvider.Type.SUBS, SkuProvider.Type.NON_CONSUMABLE
                )
            )
        )

        // in_app
        data.add(
            SkuProvider.SkuDetails(
                PREMIUM_CAR, setOf(
                    SkuProvider.Type.INAPP, SkuProvider.Type.NON_CONSUMABLE
                )
            )
        )
        data.add(
            SkuProvider.SkuDetails(
                GAS, setOf(
                    SkuProvider.Type.INAPP, SkuProvider.Type.CONSUMABLE
                )
            )
        )
    }

    override fun getAllSkus(): Set<String> {
        return data.mapTo(mutableSetOf()) { it.sku }
    }

    override fun getSkusByType(types: Set<SkuProvider.Type>): Set<String> {
        return data.filter { it.types.intersect(types).isNotEmpty() }
            .mapTo(mutableSetOf()) { it.sku }
    }

    companion object {
        const val GOLD_MONTHLY = "gold_monthly"
        const val PREMIUM_CAR = "premium_car"
        const val GAS = "gas"
    }
}