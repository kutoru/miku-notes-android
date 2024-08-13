package com.kutoru.mikunotes.ui.login

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import com.kutoru.mikunotes.R
import com.kutoru.mikunotes.databinding.ActivityLoginBinding
import com.kutoru.mikunotes.logic.BadRequest
import com.kutoru.mikunotes.logic.InvalidUrl
import com.kutoru.mikunotes.logic.LAUNCHED_LOGIN_FROM_ERROR
import com.kutoru.mikunotes.logic.ServerError
import com.kutoru.mikunotes.ui.ApiReadyActivity
import kotlinx.coroutines.launch

class LoginActivity : ApiReadyActivity<LoginViewModel>() {

    private lateinit var binding: ActivityLoginBinding
    override val viewModel: LoginViewModel by viewModels { LoginViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvLoginMessage.text = ""
        binding.btnLoginLogin.setOnClickListener { handleLogin() }
        binding.btnLoginRegister.setOnClickListener { handleRegister() }

        setSupportActionBar(binding.toolbarLogin)
        supportActionBar?.title = "Log In or Register"

        setNavigationBarColor(binding.root)

        val fromError = intent.getBooleanExtra(LAUNCHED_LOGIN_FROM_ERROR, false)
        if (fromError) {
            showMessage("The login session is invalid. Log in with your credentials")
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        moveTaskToBack(true)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.login, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.actionLoginPropertyDialog -> {
                urlDialog.show(true, null) { viewModel.updateUrl() }
            }
            else -> return super.onOptionsItemSelected(item)
        }

        return true
    }

    private fun showMessage(message: String) {
        binding.tvLoginMessage.text = message
    }

    private fun handleLogin() {
        val email = binding.etLoginEmail.text.toString().trim()
        val password = binding.etLoginPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            showMessage("Email and password cannot be empty")
            return
        }

        scope.launch {
            try {
                viewModel.postLogin(email, password)
                finish()
            } catch (e: Exception) {
                when (e) {
                    is InvalidUrl -> showMessage("Could not connect to the server. Make sure that the URL is valid and the server is running")
                    is BadRequest -> showMessage("Could not log in. Check your email and password")
                    is ServerError -> showMessage("Server error: $e")
                    else -> {
                        println("Got an unknown error on login: $e")
                        throw e
                    }
                }
            }
        }
    }

    private fun handleRegister() {
        val email = binding.etLoginEmail.text.toString().trim()
        val password = binding.etLoginPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            showMessage("Email and password cannot be empty")
            return
        }

        scope.launch {
            try {
                viewModel.postRegister(email, password)
                finish()
            } catch (e: Exception) {
                when (e) {
                    is InvalidUrl -> showMessage("Could not connect to the server. Make sure that the URL is valid and the server is running")
                    is BadRequest -> showMessage("Could not register. Your email or password might be invalid, or a user with such email might already exist")
                    is ServerError -> showMessage("Server error: $e")
                    else -> {
                        println("Got an unknown error on register: $e")
                        throw e
                    }
                }
            }
        }
    }
}
