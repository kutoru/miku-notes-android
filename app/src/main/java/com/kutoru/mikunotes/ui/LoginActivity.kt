package com.kutoru.mikunotes.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.kutoru.mikunotes.R
import com.kutoru.mikunotes.databinding.ActivityLoginBinding
import com.kutoru.mikunotes.logic.ApiService
import com.kutoru.mikunotes.logic.PersistentStorage

class LoginActivity : AppCompatActivity() {

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

        setSupportActionBar(binding.toolbarLogin)
        supportActionBar?.title = "Log in or Register"

        startApiService()
        checkBackendUrlValidity()
    }

    override fun onDestroy() {
        if (serviceIsBound) {
            unbindService(serviceConnection)
            serviceIsBound = false
        }

        super.onDestroy()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        println("onBackPressed")
        moveTaskToBack(true)
    }

    private fun startApiService() {
        val bindIntent = Intent(this, ApiService::class.java)
        bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun checkBackendUrlValidity() {
        val storage = PersistentStorage(this)
        val domain = storage.domain
        val port = storage.port
        val isSecure = storage.isSecure

        if (domain == null || port == null || isSecure == null) {
            val view = View.inflate(this, R.layout.dialog_url_properties, null)
            val etDomain = view.findViewById<EditText>(R.id.etDomain)
            val etPort = view.findViewById<EditText>(R.id.etPort)
            val cbIsSecure = view.findViewById<CheckBox>(R.id.cbIsSecure)
            val btnSubmit = view.findViewById<Button>(R.id.btnSubmit)

            if (domain != null) {
                etDomain.setText(domain)
            }
            if (port != null) {
                etPort.setText(port.toString())
            }
            if (isSecure != null) {
                cbIsSecure.isChecked = isSecure
            }

            val dialog = AlertDialog
                .Builder(this)
                .setCancelable(false)
                .setView(view)
                .show()

            btnSubmit.setOnClickListener {
                val domain = etDomain.text.toString().trim()
                if (domain.isEmpty()) {
                    Toast.makeText(this, "The domain/IP field cannot be empty", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                val port = etPort.text.toString().trim().toIntOrNull()
                if (port == null || port < 0 || port > 65000) {
                    Toast.makeText(this, "The port field should be a valid unsigned 16bit int", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                val isSecure = cbIsSecure.isChecked

                storage.domain = domain
                storage.port = port
                storage.isSecure = isSecure
                Toast.makeText(this, "The url properties have successfully been set", Toast.LENGTH_LONG).show()
                dialog.dismiss()
            }
        }
    }
}
