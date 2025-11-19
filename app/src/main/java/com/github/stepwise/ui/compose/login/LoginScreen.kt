package com.github.stepwise.ui.compose.login

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.github.stepwise.R
import com.github.stepwise.StudentActivity
import com.github.stepwise.TeacherActivity
import com.github.stepwise.network.ApiClient
import com.github.stepwise.network.models.LoginRequest
import com.github.stepwise.network.models.LoginResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

private const val TAG = "LoginScreen"
private const val PREFS_NAME = "stepwise_prefs"

@Composable
fun LoginScreen() {
    var username by remember { mutableStateOf("student") }
    var password by remember { mutableStateOf("Qq@123456") }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.primary_logo),
                        contentDescription = stringResource(R.string.app_name),
                        modifier = Modifier.size(96.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.login_welcome),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text(stringResource(R.string.hint_username)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(R.string.hint_password)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (username.isNotBlank() && password.isNotBlank()) {
                                    scope.launch {
                                        doLogin(context, username.trim(), password.trim()) { loading ->
                                            isLoading = loading
                                        }
                                    }
                                }
                            }
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (username.trim().isEmpty() || password.trim().isEmpty()) {
                                Toast.makeText(context, "Введите логин и пароль", Toast.LENGTH_SHORT).show()
                            } else {
                                scope.launch {
                                    doLogin(context, username.trim(), password.trim()) { loading ->
                                        isLoading = loading
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    ) {
                        Text(stringResource(R.string.button_login))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { /* TODO: Implement forgot password */ }) {
                            Text(
                                text = stringResource(R.string.forgot_password),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(36.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

private suspend fun doLogin(
    context: Context,
    username: String,
    password: String,
    setLoading: (Boolean) -> Unit
) {
    setLoading(true)
    try {
        Log.d(TAG, "Attempt login for user='$username'")
        val response = withContext(Dispatchers.IO) {
            ApiClient.apiService.login(LoginRequest(username, password))
        }

        if (response.isSuccessful) {
            val body: LoginResponse? = response.body()
            Log.d(TAG, "Login successful response body: $body")
            if (body != null && body.token != null) {
                val roleFromServer = body.role ?: body.user?.role ?: "STUDENT"
                saveAuth(context, body.token, roleFromServer)
                startRoleActivity(context, roleFromServer)
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Неверный ответ сервера", Toast.LENGTH_SHORT).show()
                }
                Log.e(TAG, "Response body is null or token missing: $body")
            }
        } else {
            val code = response.code()
            val errBody = try {
                response.errorBody()?.string()
            } catch (e: IOException) {
                "errorBody read failed: ${e.message}"
            }
            Log.e(TAG, "Login failed: code=$code, errorBody=$errBody")
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Ошибка входа: $code", Toast.LENGTH_LONG).show()
            }
        }
    } catch (e: Throwable) {
        Log.e(TAG, "Login exception", e)
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Ошибка сети или парсинга: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    } finally {
        setLoading(false)
    }
}

private fun saveAuth(context: Context, token: String, role: String) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putString("token", token).putString("role", role).apply()
    Log.d(TAG, "Saved token and role=$role")
}

private fun startRoleActivity(context: Context, role: String) {
    val intent = when (role.uppercase()) {
        "STUDENT" -> Intent(context, StudentActivity::class.java)
        "TEACHER" -> Intent(context, TeacherActivity::class.java)
        else -> Intent(context, StudentActivity::class.java)
    }
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    context.startActivity(intent)
}
