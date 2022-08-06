package com.example.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.example.app.R
import com.example.app.billing.BillingViewModel
import com.example.app.databinding.ActivityMainBinding
import com.example.app.ui.main.MainFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val billingViewModel: BillingViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        // initialize viewModel because by viewModel() is lazy
        billingViewModel.toString()

        val fm = supportFragmentManager
        val fragment = fm.findFragmentById(R.id.my_content)
        if (fragment == null) {
            val ft = fm.beginTransaction()
            ft.replace(
                R.id.my_content,
                MainFragment.newInstance(), MainFragment::class.java.name
            )
            ft.commit()
        }
    }
}
