package com.kutoru.mikunotes.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.kutoru.mikunotes.R
import com.kutoru.mikunotes.databinding.ActivityLoginBinding
import com.kutoru.mikunotes.logic.ApiService
import com.kutoru.mikunotes.logic.BadRequest
import com.kutoru.mikunotes.logic.InvalidUrl
import com.kutoru.mikunotes.logic.ServerError
import com.kutoru.mikunotes.logic.UrlPropertyDialog
import com.kutoru.mikunotes.models.LoginBody
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    private lateinit var binding: ActivityLoginBinding

    private lateinit var apiService: ApiService
    private var serviceIsBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as ApiService.ServiceBinder
            apiService = binder.getService()
            serviceIsBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            serviceIsBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLoginLogin.setOnClickListener { handleLogin() }
        binding.btnLoginRegister.setOnClickListener { handleRegister() }

        setSupportActionBar(binding.toolbarLogin)
        supportActionBar?.title = "Log in or Register"

        startApiService()
    }

    override fun onDestroy() {
        if (serviceIsBound) {
            unbindService(serviceConnection)
            serviceIsBound = false
        }

        job.cancel()
        super.onDestroy()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        println("onBackPressed")
        moveTaskToBack(true)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.login, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.actionLoginPropertyDialog -> {
                UrlPropertyDialog.launch(this, true, callback = { apiService.updateUrl() })
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun startApiService() {
        val bindIntent = Intent(this, ApiService::class.java)
        bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE)
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
                    is InvalidUrl -> showMessage("Could not connect to the backend. Make sure that the backend url is valid")
                    is BadRequest -> showMessage("Could not log in. Check your email and password")
                    is ServerError -> showMessage("Server error")
                    else -> {
                        println("unknown err on login: $e")
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
                    is InvalidUrl -> showMessage("Could not connect to the backend. Make sure that the backend url is valid")
                    is BadRequest -> showMessage("Could not register. Your email or password might be invalid, or the user already exists")
                    is ServerError -> showMessage("Server error")
                    else -> {
                        println("unknown err on login: $e")
                        throw e
                    }
                }
            }
        }
    }
}
