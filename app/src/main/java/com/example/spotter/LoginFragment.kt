package com.example.spotter

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.example.spotter.databinding.ActivityMainBinding
import com.example.spotter.databinding.FragmentLoginBinding
import com.example.spotter.ui.SignupFragment

class LoginFragment : Fragment() {
    private lateinit var binding: FragmentLoginBinding
    private var mainBinding : ActivityMainBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        val containerParent = container?.parent as? View
        mainBinding = containerParent?.let { ActivityMainBinding.bind(it) }

        val inputUsername = binding.inputUsername
        val inputPassword = binding.inputPassword
        val errorUsername = binding.errorUsername
        val errorPassword = binding.errorPassword
        val errorLogin = binding.errorLogin

        binding.btnLogin.setOnClickListener {
            var formOK = true

            var usernameErrorMsg : String = ""
            if (inputUsername.text.isEmpty()) {usernameErrorMsg = getString(R.string.error_username_empty); formOK = false;}

            var passwordErrorMsg : String = ""
            if (inputPassword.text.isEmpty()) {passwordErrorMsg = getString(R.string.error_password_empty); formOK = false;}

            if (formOK) {
                // do login logic here
                val loginOK = true
                if (loginOK) {
                    // go to home
                    mainBinding?.let {
                        mainBinding!!.loginContainer.visibility = View.GONE
                        mainBinding!!.navView.visibility = View.VISIBLE
                    }
                    val fragmentTransaction = requireActivity().supportFragmentManager.beginTransaction()
                    fragmentTransaction.remove(requireActivity().supportFragmentManager.findFragmentById(R.id.loginContainer)!!)
                    fragmentTransaction.commit()
                } else {
                    errorLogin.text = getString(R.string.error_unable_to_connect_to_server)
                    errorLogin.text = getString(R.string.error_failed_login)
                    errorLogin.visibility = View.VISIBLE
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

        return binding.root
    }
}