package com.kutoru.mikunotes.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kutoru.mikunotes.databinding.FragmentSettingsBinding
import com.kutoru.mikunotes.logic.PersistentStorage
import com.kutoru.mikunotes.ui.UrlPropertyDialog
import com.kutoru.mikunotes.ui.activities.LoginActivity
import com.kutoru.mikunotes.ui.activities.MainActivity
import kotlinx.coroutines.launch

class SettingsFragment : ServiceBoundFragment() {

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
                apiService.getLogout("Could not log out") ?: return@launch

                val intent = Intent(requireActivity(), LoginActivity::class.java)
                requireActivity().startActivity(intent)
            }
        }

        binding.btnSettingsEditUrl.setOnClickListener {
            UrlPropertyDialog.launch(requireContext(), null, true) { apiService.updateUrl() }
        }

        (requireActivity() as MainActivity).setSettingsOptionsMenu()

        return binding.root
    }
}
