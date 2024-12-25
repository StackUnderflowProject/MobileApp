package com.example.spotter

import android.os.Bundle
import android.view.View
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.spotter.databinding.ActivityMainBinding
import com.example.spotter.ui.SignupFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //launchFragment(LoginFragment())

        val navView: BottomNavigationView = binding.navView
        supportActionBar?.hide() // hides top left page title

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    private fun launchFragment(fragment : Fragment) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()

        if (fragment is LoginFragment) {
            fragmentTransaction.replace(binding.loginContainer.id, fragment)
            binding.navView.visibility = View.GONE
            binding.loginContainer.visibility = View.VISIBLE
        } else {
            fragmentTransaction.addToBackStack(null)
        }

        fragmentTransaction.commit()
    }
}