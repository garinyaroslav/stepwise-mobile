package com.github.stepwise.ui.compose.student

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.github.stepwise.network.ApiClient
import com.github.stepwise.network.models.WorkResponseDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentProjectsScreen(
    onNavigateToDetail: (Long) -> Unit
) {
    var works by remember { mutableStateOf<List<WorkResponseDto>>(emptyList()) }
    var isRefreshing by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val pullRefreshState = rememberPullToRefreshState()

    fun loadWorks() {
        scope.launch {
            isRefreshing = true
            try {
                val resp = withContext(Dispatchers.IO) {
                    ApiClient.apiService.getStudentWorks()
                }
                
                if (resp.isSuccessful) {
                    works = resp.body() ?: emptyList()
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Ошибка: ${resp.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Ошибка сети: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            } finally {
                isRefreshing = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadWorks()
    }
    
    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            loadWorks()
        }
    }
    
    LaunchedEffect(isRefreshing) {
        if (!isRefreshing) {
            pullRefreshState.endRefresh()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(pullRefreshState.nestedScrollConnection)
    ) {
        if (works.isEmpty() && !isRefreshing) {
            Text(
                text = "У вас пока нет работ",
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp)
            ) {
                items(works) { work ->
                    StudentWorkItem(
                        work = work,
                        onClick = { onNavigateToDetail(work.id ?: -1L) }
                    )
                }
            }
        }
        
        if (pullRefreshState.isRefreshing) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            )
        }
    }
}

@Composable
fun StudentWorkItem(
    work: WorkResponseDto,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = work.title ?: "Работа",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${work.groupName ?: ""} · ${work.teacherName ?: work.teacherEmail ?: ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            val total = work.countOfChapters ?: 0
            val progress = if (total > 0) 0f / total else 0f
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
