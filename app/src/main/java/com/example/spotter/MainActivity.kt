package com.example.spotter

import android.os.Bundle
import android.view.View
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.spotter.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var myApp: SpotterApp


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        myApp = application as SpotterApp

        myApp.user = myApp.getUser(this)
        if (myApp.user == null || myApp.user!!.jwt.isEmpty()) {
            launchFragment(LoginFragment())
        }
        //launchFragment(AddEventFragment())

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

    fun launchFragment(fragment : Fragment, backStack : Boolean = true, bundle: Bundle? = null) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()

        if (fragment is LoginFragment) {
            fragmentTransaction.replace(binding.loginContainer.id, fragment)
            binding.navView.visibility = View.GONE
            binding.loginContainer.visibility = View.VISIBLE
        } else {
            bundle?.let {fragment.arguments = bundle}
            fragmentTransaction.replace(binding.fragmentContainer.id, fragment)
            if (backStack) fragmentTransaction.addToBackStack(null)
        }

        fragmentTransaction.commit()
    }
}