package com.example.spotter

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.spotter.databinding.ActivityMainBinding
import com.example.spotter.databinding.FragmentLoginBinding
import com.example.spotter.ui.dashboard.DashboardFragment
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginFragment : Fragment() {
    private lateinit var binding: FragmentLoginBinding
    private var mainBinding : ActivityMainBinding? = null
    private lateinit var myApp: SpotterApp

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        val containerParent = container?.parent as? View
        mainBinding = containerParent?.let { ActivityMainBinding.bind(it) }

        myApp = requireActivity().application as SpotterApp

        val inputUsername = binding.inputUsername
        val inputPassword = binding.inputPassword
        val errorUsername = binding.errorUsername
        val errorPassword = binding.errorPassword
        val errorLogin = binding.errorLogin

        savedInstanceState?.let {
            val username = savedInstanceState.getString("username", "")
            inputUsername.setText(username)
        }

        binding.btnLogin.setOnClickListener {
            var formOK = true

            var usernameErrorMsg : String = ""
            if (inputUsername.text.isEmpty()) {usernameErrorMsg = getString(R.string.error_username_empty); formOK = false;}

            var passwordErrorMsg : String = ""
            if (inputPassword.text.isEmpty()) {passwordErrorMsg = getString(R.string.error_password_empty); formOK = false;}

            if (formOK) {
                login(inputUsername.text.toString(), inputPassword.text.toString()) { code ->
                    if (code == 0) {
                        // go to home
                        mainBinding?.let {
                            mainBinding!!.loginContainer.visibility = View.GONE
                            mainBinding!!.navView.visibility = View.VISIBLE
                        }
                        // clear the entire stack
                        requireActivity().supportFragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                        /*
                        val fragmentTransaction = requireActivity().supportFragmentManager.beginTransaction()
                        fragmentTransaction.remove(requireActivity().supportFragmentManager.findFragmentById(R.id.loginContainer)!!)
                        fragmentTransaction.commit()*/
                    } else {
                        if (code == 1) errorLogin.text = getString(R.string.error_failed_login)
                        if (code == 2) errorLogin.text = getString(R.string.error_unable_to_connect_to_server)
                        errorLogin.visibility = View.VISIBLE
                    }
                }
            } else {
                if (passwordErrorMsg.isNotEmpty()) {errorPassword.text = passwordErrorMsg; errorPassword.visibility = View.VISIBLE; inputPassword.requestFocus();}
                if (usernameErrorMsg.isNotEmpty()) {errorUsername.text = usernameErrorMsg; errorUsername.visibility = View.VISIBLE; inputUsername.requestFocus();}
            }
        }

        binding.btnGoToSignup.setOnClickListener {
            val fragment = SignupFragment()
            val fragmentTransaction = requireActivity().supportFragmentManager.beginTransaction()

            mainBinding?.let {
                fragmentTransaction.replace(mainBinding!!.loginContainer.id, fragment)
            }
            fragmentTransaction.addToBackStack(null)
            fragmentTransaction.commit()
        }

        // don't allow the back button navigation to main activity
        val backPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (requireActivity().supportFragmentManager.backStackEntryCount > 1) requireActivity().onBackPressed()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressedCallback)

        return binding.root
    }

    private fun login(username : String, password : String, callback: (Int) -> Unit) {
        RetrofitInstance.api.login(LOGIN_MODEL(username, password))
            .enqueue(object : Callback<User> {
                override fun onResponse(
                    call: Call<User>,
                    response: Response<User>
                ) {
                    if (response.isSuccessful) {
                        val user = response.body()
                        user!!.loginTime = System.currentTimeMillis()
                        myApp.user = user
                        myApp.storeUser(requireContext(), user!!)
                        myApp.initializeWebSocket()
                        callback(0)
                    } else {
                        Log.i("Output", "login(), Error: ${response.code()}")
                        callback(1)
                    }
                }
                override fun onFailure(call: Call<User>, t: Throwable) {
                    Log.i("Output", "Failed ${t.message}")
                    callback(2)
                }
            }
        )
    }
}
