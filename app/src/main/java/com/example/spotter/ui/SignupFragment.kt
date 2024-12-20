package com.example.spotter.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.spotter.LoginFragment
import com.example.spotter.R
import com.example.spotter.databinding.ActivityMainBinding
import com.example.spotter.databinding.FragmentSignupBinding

class SignupFragment : Fragment() {
    private lateinit var binding: FragmentSignupBinding
    private lateinit var mainBinding: ActivityMainBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSignupBinding.inflate(inflater, container, false)
        mainBinding = ActivityMainBinding.inflate(inflater)

        val inputEmail = binding.inputEmail
        val inputUsername = binding.inputUsername
        val inputPassword = binding.inputPassword
        val inputRepeatPassword = binding.inputRepeatPassword
        val errorEmail = binding.errorEmail
        val errorUsername = binding.errorUsername
        val errorPassword = binding.errorPassword
        val errorRepeatPassword = binding.errorRepeatPassword
        val errorSignup = binding.errorSignup

        binding.btnSignup.setOnClickListener {
            var formOK = true

            var emailErrorMsg : String = ""
            if (inputEmail.text.isEmpty()) {emailErrorMsg = getString(R.string.error_email_empty); formOK = false;}

            var usernameErrorMsg : String = ""
            if (inputUsername.text.isEmpty()) {usernameErrorMsg = getString(R.string.error_username_empty); formOK = false;}

            var passwordErrorMsg : String = ""
            if (inputPassword.text.isEmpty()) {passwordErrorMsg = getString(R.string.error_password_empty); formOK = false;}

            var repeatPasswordErrorMsg : String = ""
            if (inputRepeatPassword.text.toString() != inputPassword.text.toString()) {repeatPasswordErrorMsg = getString(R.string.error_passwords_not_matching); formOK = false;}
            if (inputRepeatPassword.text.isEmpty()) {repeatPasswordErrorMsg = getString(R.string.error_password_empty); formOK = false;}

            if (formOK) {
                // do signup logic here
                val signupOK = true
                if (signupOK) {
                    // go to login
                    launchLogin()
                } else {
                    errorSignup.text = getString(R.string.error_unable_to_connect_to_server)
                    errorSignup.text = getString(R.string.error_failed_login)
                    errorSignup.visibility = View.VISIBLE
                }
            } else {
                if (repeatPasswordErrorMsg.isNotEmpty()) {errorRepeatPassword.text = repeatPasswordErrorMsg; errorRepeatPassword.visibility = View.VISIBLE; inputRepeatPassword.requestFocus();}
                if (passwordErrorMsg.isNotEmpty()) {errorPassword.text = passwordErrorMsg; errorPassword.visibility = View.VISIBLE; inputPassword.requestFocus();}
                if (usernameErrorMsg.isNotEmpty()) {errorUsername.text = usernameErrorMsg; errorUsername.visibility = View.VISIBLE; inputUsername.requestFocus();}
                if (emailErrorMsg.isNotEmpty()) {errorEmail.text = emailErrorMsg; errorEmail.visibility = View.VISIBLE; inputEmail.requestFocus();}
            }
        }

        binding.btnGoToLogin.setOnClickListener {
            launchLogin()
        }

        return binding.root
    }

    private fun launchLogin() {
        val fragment = LoginFragment()
        val fragmentTransaction = requireActivity().supportFragmentManager.beginTransaction()

        fragmentTransaction.replace(mainBinding.loginContainer.id, fragment)
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
    }
}