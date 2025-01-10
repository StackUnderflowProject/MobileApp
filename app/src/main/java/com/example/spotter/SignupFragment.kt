package com.example.spotter

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.spotter.databinding.ActivityMainBinding
import com.example.spotter.databinding.FragmentSignupBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

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
                register(inputUsername.text.toString(), inputEmail.text.toString(), inputPassword.text.toString()) { code ->
                    if (code == 0) {
                        launchLogin(inputUsername.text.toString())
                    } else {
                        if (code == 1) errorSignup.text = getString(R.string.user_w_username_exists)
                        if (code == 2) errorSignup.text = getString(R.string.error_unable_to_connect_to_server)
                        errorSignup.visibility = View.VISIBLE
                    }
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

    private fun launchLogin(username: String? = null) {
        val fragment = LoginFragment()
        if (username != null) {
            val bundle = Bundle()
            bundle.putString("username", username)
            fragment.arguments = bundle
        }
        val fragmentTransaction = requireActivity().supportFragmentManager.beginTransaction()

        fragmentTransaction.replace(mainBinding.loginContainer.id, fragment)
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
    }

    private fun register(username: String, email: String, password: String, callback: (Int) -> Unit) {
        RetrofitInstance.api.register(REGISTER_MODEL(username, email, password))
            .enqueue(object : Callback<User> {
                override fun onResponse(
                    call: Call<User>,
                    response: Response<User>
                ) {
                    if (response.isSuccessful) {
                        callback(0)
                    } else {
                        Log.i("Output", "register(), Error: ${response.code()}")
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