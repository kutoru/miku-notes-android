package com.kutoru.mikunotes.ui.activities

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.kutoru.mikunotes.R
import com.kutoru.mikunotes.databinding.ActivityLoginBinding
import com.kutoru.mikunotes.logic.BadRequest
import com.kutoru.mikunotes.logic.InvalidUrl
import com.kutoru.mikunotes.logic.LAUNCHED_LOGIN_FROM_ERROR
import com.kutoru.mikunotes.logic.ServerError
import com.kutoru.mikunotes.models.LoginBody
import com.kutoru.mikunotes.ui.UrlPropertyDialog
import kotlinx.coroutines.launch

class LoginActivity : ServiceBoundActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var urlDialog: UrlPropertyDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvLoginMessage.text = ""
        binding.btnLoginLogin.setOnClickListener { handleLogin() }
        binding.btnLoginRegister.setOnClickListener { handleRegister() }

        setSupportActionBar(binding.toolbarLogin)
        supportActionBar?.title = "Log In or Register"

        urlDialog = UrlPropertyDialog(this)

        val fromError = intent.getBooleanExtra(LAUNCHED_LOGIN_FROM_ERROR, false)
        if (fromError) {
            showMessage("The login session is invalid. Log in with your credentials")
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        moveTaskToBack(true)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.login, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.actionLoginPropertyDialog -> {
                urlDialog.show(true, null) { apiService.updateUrl() }
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
                apiService.postLogin(LoginBody(email, password))
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
                apiService.postRegister(LoginBody(email, password))
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
