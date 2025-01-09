package com.example.spotter

import android.os.Bundle
import android.util.TimeUtils
import android.view.View
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.spotter.databinding.ActivityMainBinding
import com.example.spotter.ui.dashboard.DashboardFragment
import org.bson.types.ObjectId

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var myApp: SpotterApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        myApp = application as SpotterApp

        myApp.user = myApp.getUser(this)
        if (myApp.user == null || myApp.user!!.token.isEmpty() || !myApp.user!!.isLoginValid()) {
            launchFragment(LoginFragment())
        }

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

        val eventId = intent?.getStringExtra("eventNotifcation")
        if (eventId != null && savedInstanceState == null) {
            // Navigate to DashboardFragment with the eventId
            val bundle = Bundle().apply { putString("eventNotifcation", eventId) }
            navController.navigate(R.id.navigation_dashboard, bundle)
        }
    }

    fun launchFragment(fragment : Fragment, backStack : String? = null, bundle: Bundle? = null) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()

        if (fragment is LoginFragment) {
            fragmentTransaction.replace(binding.loginContainer.id, fragment)
            fragmentTransaction.addToBackStack(null)
            binding.navView.visibility = View.GONE
            binding.loginContainer.visibility = View.VISIBLE
        } else {
            bundle?.let {fragment.arguments = bundle}
            fragmentTransaction.replace(binding.fragmentContainer.id, fragment)
            if (backStack != null && backStack.isEmpty()) fragmentTransaction.addToBackStack(null)
            if (backStack != null && backStack.isNotEmpty()) fragmentTransaction.addToBackStack(backStack)
        }

        fragmentTransaction.commit()
    }
}