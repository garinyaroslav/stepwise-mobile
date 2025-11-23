package com.github.stepwise.ui.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.stepwise.StudentActivity
import com.github.stepwise.TeacherActivity
import com.github.stepwise.databinding.FragmentLoginBinding
import com.github.stepwise.network.ApiClient
import com.github.stepwise.network.models.LoginRequest
import com.github.stepwise.network.models.LoginResponse
import com.github.stepwise.network.models.ResetPasswordDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import android.util.Patterns
import androidx.core.content.ContextCompat
import com.github.stepwise.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
private const val TAG = "LoginFragment"

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val prefsName = "stepwise_prefs"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.editTextUsername.setText("student")
        binding.editTextPassword.setText("Qq@123456")

        binding.buttonLogin.setOnClickListener {
            val username = binding.editTextUsername.text.toString().trim()
            val password = binding.editTextPassword.text.toString().trim()
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Введите логин и пароль", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            doLogin(username, password)
        }

        binding.textForgot.setOnClickListener {
            showRequestResetDialog()
        }
    }

    private fun doLogin(username: String, password: String) {
        binding.progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Attempt login for user='$username'")
                val response = ApiClient.apiService.login(LoginRequest(username, password))

                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                }

                if (response.isSuccessful) {
                    val body: LoginResponse? = response.body()
                    Log.d(TAG, "Login successful response body: $body")
                    withContext(Dispatchers.Main) {
                        if (body != null && body.token != null) {
                            val roleFromServer = body.role ?: body.user?.role ?: "STUDENT"
                            saveAuth(body.token, roleFromServer)
                            startRoleActivity(roleFromServer)
                        } else {
                            Toast.makeText(requireContext(), "Неверный ответ сервера", Toast.LENGTH_SHORT).show()
                            Log.e(TAG, "Response body is null or token missing: $body")
                        }
                    }
                } else {
                    val code = response.code()
                    val errBody = try { response.errorBody()?.string() } catch (e: IOException) { "errorBody read failed: ${e.message}" }
                    Log.e(TAG, "Login failed: code=$code, errorBody=$errBody")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Ошибка входа: $code", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Login exception", e)
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Ошибка сети или парсинга: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun saveAuth(token: String, role: String) {
        val prefs = requireActivity().getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        prefs.edit().putString("token", token).putString("role", role).apply()
        Log.d(TAG, "Saved token and role=$role")
    }

    private fun startRoleActivity(role: String) {
        val ctx = requireContext()
        val intent = when (role.uppercase()) {
            "STUDENT" -> Intent(ctx, StudentActivity::class.java)
            "TEACHER" -> Intent(ctx, TeacherActivity::class.java)
            else -> Intent(ctx, StudentActivity::class.java)
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun isValidPassword(pass: String): Boolean {
        val regex = Regex("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@#\$%^&+=]).{8,}$")
        return regex.matches(pass)
    }

    private fun isValidEmail(email: String): Boolean =
        Patterns.EMAIL_ADDRESS.matcher(email).matches()

    private fun showRequestResetDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_request_reset, null)
        val etEmail = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(
            R.id.etEmailInput
        )

        val current = binding.editTextUsername.text?.toString()?.trim()
        if (!current.isNullOrBlank()) {
            etEmail.setText(current)
        }

        val alert = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Восстановление пароля")
            .setView(dialogView)
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Отправить", null)
            .create()

        alert.setOnShowListener {
            alert.window?.setBackgroundDrawable(
                ContextCompat.getDrawable(requireContext(), R.drawable.bg_dialog_rounded)
            )

            val btnPositive = alert.getButton(AlertDialog.BUTTON_POSITIVE)
            btnPositive.setOnClickListener {
                val email = etEmail.text?.toString()?.trim().orEmpty()
                if (email.isBlank()) {
                    Toast.makeText(requireContext(), "Введите email", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (!isValidEmail(email)) {
                    Toast.makeText(requireContext(), "Некорректный email", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                btnPositive.isEnabled = false
                requestPasswordReset(
                    email,
                    onDone = {
                        btnPositive.isEnabled = true
                        alert.dismiss()
                        promptForTokenAndNewPassword(email)
                    },
                    onError = {
                        btnPositive.isEnabled = true
                    }
                )
            }
        }

        alert.show()
    }
    private fun promptForTokenAndNewPassword(email: String) {
        val container = layoutInflater.inflate(R.layout.dialog_token_new_password, null)
        val etEmail = container.findViewById<EditText>(R.id.etEmail)
        val etToken = container.findViewById<EditText>(R.id.etToken)
        val etNew = container.findViewById<EditText>(R.id.etNewPassword)
        val etConfirm = container.findViewById<EditText>(R.id.etConfirmPassword)

        etEmail.setText(email)
        etEmail.isEnabled = false
        etEmail.isFocusable = false

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Ввести код и новый пароль")
            .setView(container)
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Сменить", null)
            .create()

        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawable(
                ContextCompat.getDrawable(requireContext(), R.drawable.bg_dialog_rounded)
            )

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
                performPasswordReset(token, newPass,
                    onSuccess = {
                        positive.isEnabled = true
                        Toast.makeText(requireContext(), "Пароль успешно изменён", Toast.LENGTH_LONG).show()
                        dialog.dismiss()
                    },
                    onError = {
                        positive.isEnabled = true
                    }
                )
            }
        }

        dialog.show()
    }

    private fun requestPasswordReset(
        email: String,
        onDone: () -> Unit,
        onError: () -> Unit
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val resp = ApiClient.apiService.requestPasswordReset(email)
                withContext(Dispatchers.Main) {
                    if (resp.isSuccessful) {
                        Toast.makeText(requireContext(), "Ссылка/код отправлен на $email", Toast.LENGTH_LONG).show()
                        onDone()
                    } else {
                        Toast.makeText(requireContext(), "Ошибка запроса: ${resp.code()}", Toast.LENGTH_LONG).show()
                        onError()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Ошибка сети: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    onError()
                }
            }
        }
    }

    private fun performPasswordReset(
        token: String,
        newPassword: String,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val dto = ResetPasswordDto(token = token, newPassword = newPassword)
                val resp = ApiClient.apiService.resetPassword(dto)
                withContext(Dispatchers.Main) {
                    if (resp.isSuccessful) {
                        onSuccess()
                    } else {
                        val errMsg = try {
                            resp.errorBody()?.string()?.takeIf { it.isNotBlank() } ?: "Ошибка: ${resp.code()}"
                        } catch (t: Throwable) {
                            "Ошибка: ${resp.code()}"
                        }
                        Toast.makeText(requireContext(), errMsg, Toast.LENGTH_LONG).show()
                        onError()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Ошибка сети: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    onError()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}