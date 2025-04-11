1.0.1
- wywalenie billing repo z library
- zmiana nazwy metody

1.0.2
- usuniecie publicznych pól z billing manager

1.0.3
- fix metody equals w ProductDetails, teraz porównuje tylko sku
- fix w FakeBillingManager.fetchProductDetails

1.1.0
- [BREAKING CHANGE] usuniety BillingCache
- [BREAKING CHANGE] zmiana nazwy ProductDetails na SkuDetailsSnipped
- API change: BillingManager, usuniete kilka callbackow
- API change: PlayStoreDataSource, usuniete kilka callbackow
- Internal: wewnętrzenie jest uzywany SkuDetails, na zewnątrz SkuDetailsSnipped

2.0.0
- [API change] migrate to Play Billing Library v4
- [NEW] apply quantity feature

3.0.0
- [API change] migrate to Play Billing Library v6
- [BREAKING CHANGE] sku -> product

6.2.0
- [BREAKING CHANGE] entire library redesign
- [NEW] update to Play Billing Library v6.2.0
- [NEW] version naming depends on used Play Billing Library version

6.2.0.1
- [FIX] fix crash trying to connect to billing service

7.1.1.0
- [API change] migrate to Play Billing Library v7
