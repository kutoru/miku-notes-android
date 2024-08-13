package com.kutoru.mikunotes.ui.SettingsFragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.kutoru.mikunotes.databinding.FragmentSettingsBinding
import com.kutoru.mikunotes.logic.PersistentStorage
import com.kutoru.mikunotes.ui.ApiReadyFragment
import com.kutoru.mikunotes.ui.LoginActivity.LoginActivity
import com.kutoru.mikunotes.ui.MainActivity.MainActivity
import kotlinx.coroutines.launch

class SettingsFragment : ApiReadyFragment<SettingsViewModel>() {

    private lateinit var binding: FragmentSettingsBinding

    override val viewModel: SettingsViewModel by viewModels { SettingsViewModel.Factory }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = FragmentSettingsBinding.inflate(inflater, container, false)

        refreshEmail()

        binding.btnSettingsLogout.setOnClickListener {
            scope.launch {
                val result = handleRequest { viewModel.getLogout() }
                if (result.isFailure) {
                    showToast("Could not log out")
                    return@launch
                }

                val intent = Intent(requireActivity(), LoginActivity::class.java)
                requireActivity().startActivity(intent)
            }
        }

        binding.btnSettingsEditUrl.setOnClickListener {
            urlDialog.show(true, null) { viewModel.updateUrl() }
        }

        (requireActivity() as MainActivity).setSettingsOptionsMenu()

        return binding.root
    }

    override fun onResume() {
        refreshEmail()
        super.onResume()
    }

    private fun refreshEmail() {
        val email = PersistentStorage(requireContext()).email
        binding.tvSettingsEmail.text = if (email != null) {
            "Email: $email"
        } else {
            "Email is not present"
        }
    }
}
