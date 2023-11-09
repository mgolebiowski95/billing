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
    implementation "com.android.billingclient:billing-ktx:6.0.1"
    
    implementation files("libs/billings-3.0.0.aar")
}
```

## Sample usage:
I used it with ViewModel. **BillingViewModel** holds lifecycle of PlayStore connection and is used to:
- **launchBillingFlow**
- call **fetchProductDetails** and store these results into **BillingRepository**
- **disburse consumable entitlements (consumable productIds)**
- **BillingRepository** is use to store product details and purchases (permanent purchases and subscriptions)

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

    fun launchBillingFlow(activity: Activity, productId: String) {
        viewModelScope.launch {
            billingManager.launchBillingFlow(activity, productId)
        }
    }

    override fun onBillingClientReady() {
        viewModelScope.launch {
            billingManager.fetchProductDetails(
                setOf(
                    AppProductIdProvider.GOLD_MONTHLY
                ),
                BillingClient.ProductType.SUBS
            )
            billingManager.fetchProductDetails(
                setOf(
                    AppProductIdProvider.PREMIUM_CAR,
                    AppProductIdProvider.GAS,
                ),
                BillingClient.ProductType.INAPP
            )
            billingManager.fetchPurchases()
        }
    }

    override fun onProductDetailsListChanged(productDetailsSnippetList: Set<ProductDetailsSnippet>) {
        viewModelScope.launch {
            billingRepository.updateProductDetailsSnippetList(productDetailsSnippetList.toSet())
        }
    }

    override fun onPurchasesListChanged(purchasesList: Set<String>) {
        viewModelScope.launch {
            billingRepository.updatePurchasesList(purchasesList.toSet())
        }
    }

    override fun disburseConsumableEntitlements(productId: String, quantity: Int) {
        Log.d("echo", "disburseConsumableEntitlements: productId=$productId")
    }

    override fun onPurchaseAcknowledged(productId: String) {
    }

    override fun onPurchaseConsumed(productId: String) {
    }
}
```

**BillingRepository**
```kotlin
interface BillingRepository {
    val productDetailsSnippetList: StateFlow<Set<ProductDetailsSnippet>>
    val purchasesList: StateFlow<Set<String>>

    suspend fun updateProductDetailsSnippetList(value: Set<ProductDetailsSnippet>)

    suspend fun updatePurchasesList(value: Set<String>)
}
```

**BillingRepositoryImpl**
```kotlin
class BillingRepositoryImpl : BillingRepository {
    private val _productDetailsSnippetList = MutableStateFlow<Set<ProductDetailsSnippet>(emptySet())
    override val productDetailsSnippetList: StateFlow<Set<ProductDetailsSnippet>>
        get() = _productDetailsSnippetList
    private val _purchasesList = MutableStateFlow<Set<String>>(emptySet())
    override val purchasesList: StateFlow<Set<String>>
        get() = _purchasesList

    override suspend fun updateProductDetailsSnippetList(value: Set<ProductDetailsSnippet>) {
        _productDetailsSnippetList.emit(value)
    }

    override suspend fun updatePurchasesList(value: Set<String>) {
        _purchasesList.emit(value)
    }
}
```

**DI (Koin)**
```kotlin
val billingModule = module {
    single<ProductIdProvider> { AppProductIdProvider() }
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

private fun provideFakeBillingManager(productIdProvider: ProductIdProvider): BillingManager {
    val impl = FakeBillingManager(productIdProvider)
    impl.addProductDetails(
        createFakeProductDetails(
            AppProductIdProvider.GOLD_MONTHLY,
            BillingClient.ProductType.SUBS
        )
    )
    impl.addProductDetails(
        createFakeProductDetails(
            AppProductIdProvider.PREMIUM_CAR,
            BillingClient.ProductType.INAPP
        )
    )
    impl.addProductDetails(
        createFakeProductDetails(
            AppProductIdProvider.GAS,
            BillingClient.ProductType.INAPP
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

To test flow application without real billing implementation you can create fake implementation of:
- **ProductIdProvider** // holds all product ids used in the application
- **KeyProvider** // provide licence key from the Play Developer Console
- **PurchaseValidator** // purchase validation
- **BillingRepository** // store of product details and purchases
- **BillingManager**, // main engine :-)
