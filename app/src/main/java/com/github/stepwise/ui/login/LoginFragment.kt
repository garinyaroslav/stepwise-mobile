package com.github.stepwise.ui.login

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.github.stepwise.StudentActivity
import com.github.stepwise.TeacherActivity
import com.github.stepwise.databinding.FragmentLoginBinding
import com.github.stepwise.network.ApiClient
import com.github.stepwise.network.models.LoginRequest
import com.github.stepwise.network.models.LoginResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

private const val TAG = "LoginFragment"
private const val CHANNEL_ID = "stepwise_alerts"
private const val NOTIFICATION_ID_TEMP_PW = 1001

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val prefsName = "stepwise_prefs"

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (!isGranted) {
                Toast.makeText(requireContext(), "Разрешение на уведомления не предоставлено", Toast.LENGTH_SHORT).show()
            }
        }

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

                            if (body.temporaryPassword == true) {
                                createNotificationChannelIfNeeded()

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val has = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                                    if (!has) {
                                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }

                                showTemporaryPasswordNotification(roleFromServer)
                            }

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

    private fun showTemporaryPasswordNotification(role: String) {
        val ctx = requireContext().applicationContext

        val targetIntent = when (role.uppercase()) {
            "STUDENT" -> Intent(ctx, StudentActivity::class.java)
            "TEACHER" -> Intent(ctx, TeacherActivity::class.java)
            else -> Intent(ctx, StudentActivity::class.java)
        }.apply {
            putExtra("openProfile", true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent = PendingIntent.getActivity(ctx, 0, targetIntent, pendingFlags)

        val title = "Внимание!"
        val text = "Вы используете временный пароль, пожалуйста перейдите в профиль и замените пароль."

        val builder = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(com.github.stepwise.R.drawable.primary_logo)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        with(NotificationManagerCompat.from(ctx)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    Log.w(TAG, "POST_NOTIFICATIONS permission not granted — notification not shown")
                    return
                }
            }
            notify(NOTIFICATION_ID_TEMP_PW, builder.build())
        }
    }

    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Stepwise уведомления"
            val descriptionText = "Канал для важных уведомлений приложения Stepwise"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
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