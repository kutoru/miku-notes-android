package com.kutoru.mikunotes.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.kutoru.mikunotes.databinding.FragmentSettingsBinding
import com.kutoru.mikunotes.logic.InvalidUrl
import com.kutoru.mikunotes.logic.PersistentStorage
import com.kutoru.mikunotes.logic.ServerError
import com.kutoru.mikunotes.logic.Unauthorized
import com.kutoru.mikunotes.logic.UrlPropertyDialog
import kotlinx.coroutines.launch

class SettingsFragment : CustomFragment() {

    private lateinit var binding: FragmentSettingsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = FragmentSettingsBinding.inflate(inflater, container, false)

        val email = PersistentStorage(requireContext()).email
        binding.tvSettingsEmail.text = if (email != null) "Email: $email" else "Email is not present"

        binding.btnSettingsLogout.setOnClickListener {
            scope.launch {
                handleRequest { apiService.getLogout() }
                val intent = Intent(requireActivity(), LoginActivity::class.java)
                requireActivity().startActivity(intent)
            }
        }

        binding.btnSettingsEditUrl.setOnClickListener {
            UrlPropertyDialog.launch(requireContext(), true, null) { apiService.updateUrl() }
        }

        (requireActivity() as MainActivity).setSettingsOptionsMenu()

        return binding.root
    }

    private suspend fun <T>handleRequest(requestFn: (suspend () -> T)): T? {
        try {
            return requestFn()
        } catch(e: Exception) {
            when (e) {
                is InvalidUrl -> {
                    UrlPropertyDialog.launch(
                        requireContext(),
                        false,
                        "Could not connect to the backend, make sure that the url properties are correct",
                    )

                    return null
                }

                is Unauthorized -> {
                    val intent = Intent(requireActivity(), LoginActivity::class.java)
                    requireActivity().startActivity(intent)
                    return null
                }

                is UnknownError, is ServerError -> {
                    println("Unknown error when calling backend: $e")
                    Toast.makeText(requireContext(), "Unknown error when calling backend: $e", Toast.LENGTH_LONG).show()
                    return null
                }

                else -> throw e
            }
        }
    }
}
