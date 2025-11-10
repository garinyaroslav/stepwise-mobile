package com.github.stepwise.ui.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.stepwise.MainActivity
import com.github.stepwise.databinding.FragmentProfileBinding
import com.github.stepwise.network.ApiClient
import com.github.stepwise.network.AuthInterceptor
import com.github.stepwise.network.models.ProfileReq
import com.github.stepwise.network.models.ResetPasswordDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private var argUserId: Long = -1L

    private var myEmail: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            argUserId = it.getLong("userId", -1L)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonCancel.setOnClickListener { loadProfile() }
        binding.buttonSave.setOnClickListener { saveProfile() }
        binding.buttonResetPassword.setOnClickListener { showResetPasswordDialog() }

        binding.buttonLogout.setOnClickListener { performLogout() }

        loadProfile()
    }

    private fun isValidPassword(pw: String): Boolean {
        if (pw.length < 8) return false
        val regex = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#\$%^&+=]).{8,}$")
        return regex.matches(pw)
    }

    private fun showResetPasswordDialog() {
        val options = arrayOf("Запросить ссылку сброса по email", "Ввести код и новый пароль")
        AlertDialog.Builder(requireContext())
            .setTitle("Сброс пароля")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> promptForEmailAndRequestReset()
                    1 -> promptForTokenAndNewPassword()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun promptForEmailAndRequestReset() {
        val input = EditText(requireContext()).apply {
            hint = "Email"
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setText(myEmail ?: "")
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Запросить ссылку сброса")
            .setView(input)
            .setPositiveButton("Отправить") { _, _ ->
                val email = input.text?.toString()?.trim().orEmpty()
                if (email.isBlank()) {
                    Toast.makeText(requireContext(), "Введите email", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                requestPasswordReset(email)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun promptForTokenAndNewPassword() {
        val container = layoutInflater.inflate(com.github.stepwise.R.layout.dialog_token_new_password, null)
        val etEmail = container.findViewById<EditText>(com.github.stepwise.R.id.etEmail)
        val etToken = container.findViewById<EditText>(com.github.stepwise.R.id.etToken)
        val etNew = container.findViewById<EditText>(com.github.stepwise.R.id.etNewPassword)
        val etConfirm = container.findViewById<EditText>(com.github.stepwise.R.id.etConfirmPassword)

        etEmail.setText(myEmail ?: "")
        etEmail.isEnabled = false
        etEmail.isFocusable = false

        val builder = AlertDialog.Builder(requireContext())
            .setTitle("Ввести код и новый пароль")
            .setView(container)
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Сменить", null)

        val dialog = builder.create()
        dialog.setOnShowListener {
            val positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positive.setOnClickListener {
                val token = etToken.text?.toString()?.trim().orEmpty()
                val newPass = etNew.text?.toString()?.trim().orEmpty()
                val confirm = etConfirm.text?.toString()?.trim().orEmpty()

                if (token.isBlank() || newPass.isBlank() || confirm.isBlank()) {
                    Toast.makeText(requireContext(), "Заполните все поля", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (newPass != confirm) {
                    Toast.makeText(requireContext(), "Пароли не совпадают", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (!isValidPassword(newPass)) {
                    Toast.makeText(requireContext(),
                        "Пароль не соответствует требованиям: минимум 8 символов, одна заглавная, одна строчная, одна цифра и один спецсимвол.",
                        Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                positive.isEnabled = false

                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val dto = ResetPasswordDto(token = token, newPassword = newPass)
                        val resp = ApiClient.apiService.resetPassword(dto)

                        withContext(Dispatchers.Main) {
                            positive.isEnabled = true
                            if (resp.isSuccessful) {
                                Toast.makeText(requireContext(), "Пароль успешно изменён", Toast.LENGTH_LONG).show()
                                dialog.dismiss()
                            } else {
                                val errMsg = try {
                                    resp.errorBody()?.string()?.takeIf { it.isNotBlank() } ?: "Ошибка: ${resp.code()}"
                                } catch (t: Throwable) {
                                    "Ошибка: ${resp.code()}"
                                }
                                Toast.makeText(requireContext(), errMsg, Toast.LENGTH_LONG).show()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            positive.isEnabled = true
                            Toast.makeText(requireContext(), "Ошибка сети: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }

        dialog.show()
    }
    private fun requestPasswordReset(email: String) {
        binding.buttonResetPassword.isEnabled = false
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val resp = ApiClient.apiService.requestPasswordReset(email)
                withContext(Dispatchers.Main) {
                    binding.buttonResetPassword.isEnabled = true
                    if (resp.isSuccessful) {
                        Toast.makeText(requireContext(), "Ссылка сброса отправлена на $email", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(requireContext(), "Ошибка запроса: ${resp.code()}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.buttonResetPassword.isEnabled = true
                    Toast.makeText(requireContext(), "Ошибка сети: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun performPasswordReset(token: String, newPassword: String) {
        binding.buttonResetPassword.isEnabled = false
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val dto = ResetPasswordDto(token = token, newPassword = newPassword)
                val resp = ApiClient.apiService.resetPassword(dto)

                withContext(Dispatchers.Main) {
                    binding.buttonResetPassword.isEnabled = true
                    if (resp.isSuccessful) {
                        Toast.makeText(requireContext(), "Пароль успешно изменён", Toast.LENGTH_LONG).show()
                    } else {
                        val errMsg = try {
                            resp.errorBody()?.string()?.takeIf { it.isNotBlank() } ?: "Ошибка: ${resp.code()}"
                        } catch (t: Throwable) {
                            "Ошибка: ${resp.code()}"
                        }
                        Toast.makeText(requireContext(), errMsg, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.buttonResetPassword.isEnabled = true
                    Toast.makeText(requireContext(), "Ошибка сети: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    private fun performLogout() {
        val prefs = requireActivity().getSharedPreferences("stepwise_prefs", Context.MODE_PRIVATE)
        prefs.edit().remove("token").remove("role").apply()
        val intentLogout = Intent(AuthInterceptor.ACTION_LOGOUT)
        requireActivity().sendBroadcast(intentLogout)

        val launchIntent = requireActivity().packageManager.getLaunchIntentForPackage(requireActivity().packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(launchIntent)
        } else {
            val fallback = Intent(requireContext(), MainActivity::class.java)
            fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(fallback)
        }
        requireActivity().finish()
    }

    private fun loadProfile() {
        binding.buttonSave.isEnabled = false
        binding.buttonCancel.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val resp = ApiClient.apiService.getMyProfile()
                withContext(Dispatchers.Main) {
                    binding.buttonSave.isEnabled = true
                    binding.buttonCancel.isEnabled = true
                }
                if (resp.isSuccessful) {
                    val p = resp.body()
                    myEmail = p?.email
                    val role = requireActivity().getSharedPreferences("stepwise_prefs", Context.MODE_PRIVATE).getString("role", "Student")
                    withContext(Dispatchers.Main) {
                        binding.textRole.text = if (role == "STUDENT") "Студент" else "Преподаватель"
                        binding.etFirstName.setText(p?.firstName ?: "")
                        binding.etMiddleName.setText(p?.middleName ?: "")
                        binding.etLastName.setText(p?.lastName ?: "")
                        binding.etPhone.setText(p?.phoneNumber ?: "")
                        binding.etAddress.setText(p?.address ?: "")
                        binding.textAccount.text = "${p?.username ?: ""}\n${p?.email ?: ""}"
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
        val middle = binding.etMiddleName.text?.toString()?.trim() ?: ""
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

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val dto = ProfileReq(id = null, firstName = first, lastName = last, middleName = if (middle.isBlank()) null else middle, phoneNumber = phone, address = address)
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