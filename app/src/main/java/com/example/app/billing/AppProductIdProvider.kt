package com.example.app.billing

import com.mgsoftware.billing.api.ProductIdProvider

class AppProductIdProvider : ProductIdProvider {

    override fun getKnownInappProductIds(): Set<String> {
        return setOf(PREMIUM_CAR, GAS)
    }

    override fun getKnownSubscriptionProductIds(): Set<String> {
        return setOf(GOLD_MONTHLY)
    }

    override fun getAutoConsumeProductIds(): Set<String> {
        return setOf(GAS)
    }

    companion object {
        const val GOLD_MONTHLY = "gold_monthly"
        const val PREMIUM_CAR = "premium_car"
        const val GAS = "gas"
    }
}