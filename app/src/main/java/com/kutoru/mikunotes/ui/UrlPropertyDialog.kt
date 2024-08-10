package com.kutoru.mikunotes.ui

import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.kutoru.mikunotes.R
import com.kutoru.mikunotes.logic.PersistentStorage

class UrlPropertyDialog(
    private val context: Context,
) {

    private val storage = PersistentStorage(context)
    private val dialog: AlertDialog

    private val etDomain: EditText
    private val etPort: EditText
    private val cbIsSecure: CheckBox
    private val tvMessage: TextView
    private val btnSave: Button

    init {
        val view = View.inflate(context, R.layout.dialog_url_properties, null)
        etDomain = view.findViewById(R.id.etDomain)
        etPort = view.findViewById(R.id.etPort)
        cbIsSecure = view.findViewById(R.id.cbIsSecure)
        tvMessage = view.findViewById(R.id.tvMessage)
        btnSave = view.findViewById(R.id.btnSave)

        dialog = AlertDialog
            .Builder(context)
            .setView(view)
            .create()
    }

    fun show(cancelable: Boolean, customMessage: String?, onSave: (() -> Unit)?) {
        if (dialog.isShowing) {
            dialog.dismiss()
        }

        val domain = storage.domain
        val port = storage.port
        val isSecure = storage.isSecure

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

        dialog.setCancelable(cancelable)

        btnSave.setOnClickListener {
            onButtonSaveClick()
            onSave?.invoke()
        }

        dialog.show()
    }

    fun dismiss() {
        dialog.dismiss()
    }

    private fun onButtonSaveClick() {
        val domain = etDomain.text.toString().trim()
        if (domain.isEmpty()) {
            Toast.makeText(context, "The domain/IP field cannot be empty", Toast.LENGTH_LONG).show()
            return
        }

        val port = etPort.text.toString().trim().toUShortOrNull()
        if (port == null) {
            Toast.makeText(context, "The port field must contain a valid port", Toast.LENGTH_LONG).show()
            return
        }

        val isSecure = cbIsSecure.isChecked

        storage.domain = domain
        storage.port = port.toInt()
        storage.isSecure = isSecure

        Toast.makeText(context, "URL properties saved", Toast.LENGTH_SHORT).show()
        dialog.dismiss()
    }
}
