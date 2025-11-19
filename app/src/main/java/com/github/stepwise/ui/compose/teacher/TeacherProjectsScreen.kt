package com.github.stepwise.ui.compose.teacher

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TeacherProjectsScreen(onNavigateToWorkDetail: (Long) -> Unit) {
    // TODO: Implement teacher projects screen
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Проекты по группам",
            style = MaterialTheme.typography.headlineMedium
        )
    }
}
