package com.github.stepwise.ui.compose.teacher

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CreateWorkScreen() {
    // TODO: Implement create work screen
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Создать учебную работу",
            style = MaterialTheme.typography.headlineMedium
        )
    }
}
