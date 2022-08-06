# billing
**billing** is a wrapped **play billing library** (current v4.0.0)
### WARNING
##### !!! Before usage, you need a basic knowlange about and how works Play Billing Library. Write own sample application and try it. !!! Review code and maybe you write better one :-)

## Installation

Library is installed by putting aar file into libs folder:

```
module/libs (ex. app/libs)
```

and adding the aar dependency to your `build.gradle` file:
```groovy
dependencies {
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4"
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4"
    implementation "com.android.billingclient:billing-ktx:4.0.0"
    
    implementation files("libs/billings-2.0.0.aar")
}
```

## Sample usage:
I used it with ViewModel. **BillingViewModel** holds lifecycle of PlayStore connection and is used to:
- **launchBillingFlow**
- call **fetchProductDetails** and store these results into **BillingRepository**
- **disburse consumable entitlements (consumable skus)**
- **BillingRepository** is use to store sku details and purchases (permanent purchases and subscriptions)

**BillingViewModel**
```kotlin
class BillingViewModel(
    private val billingManager: BillingManager,
    private val billingRepository: BillingRepository
) : ViewModel(), BillingManager.Listener {

    init {
        billingManager.registerListener(this)
        billingManager.openPlayStoreConnection()
    }

    override fun onCleared() {
        super.onCleared()
        billingManager.unregisterListener(this)
        billingManager.closePlayStoreConnection()
    }

    fun launchBillingFlow(activity: Activity, sku: String) {
        viewModelScope.launch {
            billingManager.launchBillingFlow(activity, sku)
        }
    }

    override fun onBillingClientReady() {
        viewModelScope.launch {
            billingManager.fetchProductDetails(
                setOf(
                    AppSkuProvider.GOLD_MONTHLY
                ),
                BillingClient.SkuType.SUBS
            )
            billingManager.fetchProductDetails(
                setOf(
                    AppSkuProvider.PREMIUM_CAR,
                    AppSkuProvider.GAS,
                ),
                BillingClient.SkuType.INAPP
            )
            billingManager.fetchPurchases()
        }
    }

    override fun onProductDetailsListChanged(skuDetailsSnippetList: Set<SkuDetailsSnippet>) {
        viewModelScope.launch {
            billingRepository.updateSkuDetailsSnippetList(skuDetailsSnippetList.toSet())
        }
    }

    override fun onPurchasesListChanged(purchasesList: Set<String>) {
        viewModelScope.launch {
            billingRepository.updatePurchasesList(purchasesList.toSet())
        }
    }

    override fun disburseConsumableEntitlements(sku: String, quantity: Int) {
        Log.d("echo", "disburseConsumableEntitlements: sku=$sku")
    }

    override fun onPurchaseAcknowledged(sku: String) {
    }

    override fun onPurchaseConsumed(sku: String) {
    }
}
```

**BillingRepository**
```kotlin
interface BillingRepository {
    val skuDetailsSnippetList: StateFlow<Set<SkuDetailsSnippet>>
    val purchasesList: StateFlow<Set<String>>

    suspend fun updateSkuDetailsSnippetList(value: Set<SkuDetailsSnippet>)

    suspend fun updatePurchasesList(value: Set<String>)
}
```

**BillingRepositoryImpl**
```kotlin
class BillingRepositoryImpl : BillingRepository {
    private val _skuDetailsSnippetList = MutableStateFlow<Set<SkuDetailsSnippet>>(emptySet())
    override val skuDetailsSnippetList: StateFlow<Set<SkuDetailsSnippet>>
        get() = _skuDetailsSnippetList
    private val _purchasesList = MutableStateFlow<Set<String>>(emptySet())
    override val purchasesList: StateFlow<Set<String>>
        get() = _purchasesList

    override suspend fun updateSkuDetailsSnippetList(value: Set<SkuDetailsSnippet>) {
        _skuDetailsSnippetList.emit(value)
    }

    override suspend fun updatePurchasesList(value: Set<String>) {
        _purchasesList.emit(value)
    }
}
```

**DI (Koin)**
```kotlin
val billingModule = module {
    single<SkuProvider> { AppSkuProvider() }
    single<KeyProvider> { NativeKeyProvider() }
    single<PurchaseValidator> { DefaultPurchaseValidator(get()) }
    single { PlayStoreDataSource(androidApplication(), get(), get()) }
    single<BillingManager> {
        val impl = DefaultBillingManager(get())
        impl
    }
    
    single<BillingRepository> { BillingRepositoryImpl() }
    viewModel { BillingViewModel(get(), get()) }
}

private fun provideFakeBillingManager(skuProvider: SkuProvider): BillingManager {
    val impl = FakeBillingManager(skuProvider)
    impl.addProductDetails(
        createFakeProductDetails(
            AppSkuProvider.GOLD_MONTHLY,
            BillingClient.SkuType.SUBS
        )
    )
    impl.addProductDetails(
        createFakeProductDetails(
            AppSkuProvider.PREMIUM_CAR,
            BillingClient.SkuType.INAPP
        )
    )
    impl.addProductDetails(
        createFakeProductDetails(
            AppSkuProvider.GAS,
            BillingClient.SkuType.INAPP
        )
    )
    return impl
}
```

**Activity**
```kotlin
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val billingViewModel: BillingViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        // initialize viewModel because 'by viewModel()'' is lazy
        billingViewModel.toString()
    }
}
```

To test flow application without real billing implementation you cal create fake implementation of:
- **SkuProvider** // holds all skus used in the application
- **KeyProvider** // provide livence key from the Play Developer Console
- **PurchaseValidator** // purchase validation
- **BillingRepository** // store of sku details and purchases
- **BillingManager**, // main engine :-)
