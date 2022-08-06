package com.example.app.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.app.R
import com.example.app.billing.BillingViewModel
import com.example.app.databinding.FragmentMainBinding
import com.example.app.ui.common.messager.Messenger
import com.example.app.ui.views.produktlist.ProductList
import com.example.app.ui.views.produktlist.ProductListImpl
import com.example.app.utility.Values
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainFragment : Fragment() {
    private lateinit var binding: FragmentMainBinding

    private lateinit var productList: ProductList

    private val viewModel: MainFragmentViewModel by viewModel()
    private val billingViewModel: BillingViewModel by sharedViewModel()

    private val values: Values by inject()
    private val messenger: Messenger by inject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_main, container, false)

        val parent = binding.productListContainer
        productList = ProductListImpl(
            inflater,
            parent,
            values,
            onItemClick = { item ->
                if (item.canBePurchased)
                    billingViewModel.launchBillingFlow(requireActivity(), item.sku)
                else
                    messenger.showMessage("Item already owned.", Toast.LENGTH_SHORT)
            })
        parent.addView(productList.getRootView())

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.items.collect { items->
                    productList.bind(items)
                }
            }
        }
    }

    companion object {
        fun newInstance() = MainFragment()
    }
}
