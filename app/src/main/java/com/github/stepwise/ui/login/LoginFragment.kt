package com.github.stepwise.ui.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.github.stepwise.databinding.FragmentLoginBinding
import com.github.stepwise.network.ApiClient
import com.github.stepwise.network.models.LoginRequest
import com.github.stepwise.network.models.LoginResponse
import com.github.stepwise.StudentActivity
import com.github.stepwise.TeacherActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

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

        binding.editTextUsername.setText("garin")
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}