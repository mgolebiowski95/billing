package com.mgsoftware.billing.impl

import com.mgsoftware.billing.KeyProvider

class FakeKeyProvider : KeyProvider {

    override fun getLicenceKey(): String {
        return ""
    }
}