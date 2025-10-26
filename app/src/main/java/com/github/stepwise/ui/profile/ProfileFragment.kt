package com.github.stepwise.ui.profile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.github.stepwise.databinding.FragmentProfileBinding
import com.github.stepwise.network.ApiClient
import com.github.stepwise.network.models.ProfileReq
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private var argUserId: Long = -1L
// TODO: create logout
//    fun logout(context: Context) {
//        val prefs = context.getSharedPreferences("stepwise_prefs", Context.MODE_PRIVATE)
//        prefs.edit().remove("token").remove("role").apply()
//        context.sendBroadcast(Intent(com.github.stepwise.network.AuthInterceptor.ACTION_LOGOUT))
//    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonCancel.setOnClickListener { loadProfile() }
        binding.buttonSave.setOnClickListener { saveProfile() }
        binding.buttonResetPassword.setOnClickListener { Toast.makeText(requireContext(), "Change password on server (not implemented)", Toast.LENGTH_SHORT).show() }

        loadProfile()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            argUserId = it.getLong("userId", -1L)
        }
    }

    private fun loadProfile() {
        binding.buttonSave.isEnabled = false
        binding.buttonCancel.isEnabled = false
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resp = ApiClient.apiService.getMyProfile()
                withContext(Dispatchers.Main) {
                    binding.buttonSave.isEnabled = true
                    binding.buttonCancel.isEnabled = true
                }
                if (resp.isSuccessful) {
                    val p = resp.body()
                    withContext(Dispatchers.Main) {
                        binding.etFirstName.setText(p?.firstName ?: "")
                        binding.etLastName.setText(p?.lastName ?: "")
                        binding.etPhone.setText(p?.phoneNumber ?: "")
                        binding.etAddress.setText(p?.address ?: "")
                        binding.textAccount.text = "${p?.username ?: ""} • ${p?.email ?: ""}"
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Ошибка загрузки профиля: ${resp.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Ошибка сети: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveProfile() {
        val first = binding.etFirstName.text?.toString()?.trim() ?: ""
        val last = binding.etLastName.text?.toString()?.trim() ?: ""
        val phone = binding.etPhone.text?.toString()?.trim() ?: ""
        val address = binding.etAddress.text?.toString()?.trim() ?: ""

        if (first.length < 2) { binding.tilFirstName.error = "Min 2 chars"; return }
        if (last.length < 2) { binding.tilLastName.error = "Min 2 chars"; return }
        if (!phone.matches(Regex("^(\\+7|8)[0-9]{10}$"))) { binding.tilPhone.error = "Phone must start with +7 or 8 and have 10 digits"; return }

        binding.tilFirstName.error = null
        binding.tilLastName.error = null
        binding.tilPhone.error = null

        binding.buttonSave.isEnabled = false
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dto = ProfileReq(id = null, firstName = first, lastName = last, phoneNumber = phone, address = address)
                val resp = ApiClient.apiService.updateProfile(dto)
                withContext(Dispatchers.Main) {
                    binding.buttonSave.isEnabled = true
                    if (resp.isSuccessful) {
                        Toast.makeText(requireContext(), "Profile saved", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Save error: ${resp.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.buttonSave.isEnabled = true
                    Toast.makeText(requireContext(), "Ошибка: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}