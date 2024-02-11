package com.mgsoftware.billing.api

interface ProductIdProvider {

    fun getKnownInappProductIds(): Set<String>
    fun getKnownSubscriptionProductIds(): Set<String>
    fun getAutoConsumeProductIds(): Set<String>
}