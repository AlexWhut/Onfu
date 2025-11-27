package com.onfu.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.onfu.app.R
import com.onfu.app.databinding.ActivityMainBinding
import com.onfu.app.ui.fragments.LoginFragment
import com.onfu.app.ui.fragments.PlaceholderFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Only add the default fragment the first time
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, LoginFragment())
                .commit()
        }

        binding.btnLogin.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, LoginFragment())
                .commit()
        }

        binding.btnRegister.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, PlaceholderFragment.newInstance("Register"))
                .commit()
        }

        binding.btnFeed.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, PlaceholderFragment.newInstance("Feed"))
                .commit()
        }

        binding.btnHome.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, PlaceholderFragment.newInstance("Home"))
                .commit()
        }

        binding.btnSearch.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, PlaceholderFragment.newInstance("Search"))
                .commit()
        }

        binding.btnProfile.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, PlaceholderFragment.newInstance("Profile"))
                .commit()
        }
    }
}


