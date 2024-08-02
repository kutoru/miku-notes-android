package com.kutoru.mikunotes.logic

import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.kutoru.mikunotes.R

class UrlPropertyDialog {
    companion object {
        fun launch(context: Context, customMessage: String? = null, callback: (() -> Unit)? = null) {
            val storage = PersistentStorage(context)
            val domain = storage.domain
            val port = storage.port
            val isSecure = storage.isSecure

            val view = View.inflate(context, R.layout.dialog_url_properties, null)
            val etDomain = view.findViewById<EditText>(R.id.etDomain)
            val etPort = view.findViewById<EditText>(R.id.etPort)
            val cbIsSecure = view.findViewById<CheckBox>(R.id.cbIsSecure)
            val tvMessage = view.findViewById<TextView>(R.id.tvMessage)
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

            if (customMessage != null) {
                tvMessage.visibility = View.VISIBLE
                tvMessage.text = customMessage
            } else {
                tvMessage.visibility = View.GONE
            }

            val dialog = AlertDialog
                .Builder(context)
                .setCancelable(false)
                .setView(view)
                .show()

            btnSubmit.setOnClickListener {
                val domain = etDomain.text.toString().trim()
                if (domain.isEmpty()) {
                    Toast.makeText(context, "The domain/IP field cannot be empty", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                val port = etPort.text.toString().trim().toIntOrNull()
                if (port == null || port < 0 || port > 65000) {
                    Toast.makeText(context, "The port field should be a valid unsigned 16bit int", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                val isSecure = cbIsSecure.isChecked

                storage.domain = domain
                storage.port = port
                storage.isSecure = isSecure

                Toast.makeText(context, "The url properties have successfully been set", Toast.LENGTH_LONG).show()
                dialog.dismiss()

                if (callback != null) {
                    callback()
                }
            }
        }
    }
}
