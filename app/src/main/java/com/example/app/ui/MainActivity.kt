package com.example.app.ui

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.example.app.R
import com.example.app.billing.BillingViewModel
import com.example.app.databinding.ActivityMainBinding
import com.example.app.ui.common.appstate.AppStateManager
import com.example.app.ui.views.produktlist.Item
import com.example.app.ui.views.produktlist.ProductList
import com.example.app.ui.views.produktlist.ProductListImpl
import com.example.app.utility.Values
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named
import timber.log.Timber

class MainActivity : AppCompatActivity(), ProductList.Listener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var productList: ProductList

    private val values: Values by inject()

    private val billingViewModel: BillingViewModel = get()
    private val viewModel: MainActivityViewModel by inject()

    private val appStateManager: AppStateManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.root.applyInsets()

        productList = ProductListImpl(
            inflater = layoutInflater,
            parent = binding.productListContainer,
            values = values,
        )
        binding.productListContainer.addView(productList.getRootView())

        lifecycleScope.launch {
            viewModel.items.collect {
                productList.bind(it)
            }
        }



        binding.openConnectionButton.setOnClickListener {
            billingViewModel.openConnection()
        }

        binding.closeConnectionButton.setOnClickListener {
            billingViewModel.closeConnection()
        }

        lifecycleScope.launch {
            appStateManager.onAppForegrounded.drop(1).collect {
                billingViewModel.fetchPurchases()
            }
        }

        lifecycleScope.launch {
            billingViewModel.connectionState.collect {
                println("echo: $it")
            }
        }
    }

    override fun onStart() {
        super.onStart()
        productList.registerListener(this)
        Timber.d("onStart")
    }

    override fun onStop() {
        super.onStop()
        productList.unregisterListener(this)
    }

    override fun onItemClick(item: Item) {
        billingViewModel.launchBillingFlow(this, item.productId)
    }

    fun View.applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(this) { v, windowInsets ->
            val insets =
                windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = insets.left
                topMargin = insets.top
                rightMargin = insets.right
                bottomMargin = insets.bottom
            }
            WindowInsetsCompat.CONSUMED
        }
    }
}