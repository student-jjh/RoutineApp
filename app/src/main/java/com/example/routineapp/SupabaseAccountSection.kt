package com.example.routineapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudDone
import com.example.routineapp.data.SupabaseAuth
import com.example.routineapp.data.SupabaseConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun SupabaseAccountDialog(config: SupabaseConfig, onDismiss: () -> Unit, onSessionChanged: () -> Unit = {}) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = { SupabaseAccountSection(config, onSessionChanged) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("닫기") } }
    )
}

@Composable
fun SupabaseAccountSection(config: SupabaseConfig, onSessionChanged: () -> Unit = {}) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val auth = remember(config) { SupabaseAuth(context, config) }
    val scope = rememberCoroutineScope()
    var session by remember { mutableStateOf(auth.currentSession()) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    fun launchAuth(block: suspend () -> Unit) {
        busy = true
        message = null
        scope.launch {
            try { block() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { message = error.message ?: "인증에 실패했어요." }
            finally { busy = false }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        if (session == null) Icons.Default.AccountCircle else Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(9.dp)
                    )
                }
                Column(Modifier.padding(start = 12.dp).weight(1f)) {
                    Text("계정 연결", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (session == null) "기록을 클라우드에 보관하세요"
                        else "클라우드 백업 사용 중",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (session != null) {
                Text(session!!.user.email ?: "이메일 계정", style = MaterialTheme.typography.bodyMedium)
                TextButton(enabled = !busy, onClick = {
                    busy = true
                    scope.launch {
                        try {
                            auth.signOut()
                            session = null
                            onSessionChanged()
                            message = "로그아웃했어요."
                        } finally { busy = false }
                    }
                }, modifier = Modifier.align(Alignment.End)) { Text("로그아웃") }
            } else {
                OutlinedTextField(value = email, onValueChange = { email = it }, enabled = !busy,
                    singleLine = true, label = { Text("이메일 주소") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = password, onValueChange = { password = it }, enabled = !busy,
                    singleLine = true, visualTransformation = PasswordVisualTransformation(),
                    label = { Text("비밀번호") }, supportingText = { Text("6자 이상") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(enabled = !busy, onClick = { launchAuth {
                        val result = auth.signIn(email, password)
                        session = result.session
                        onSessionChanged()
                        message = result.message
                    } }, modifier = Modifier.weight(1f)) { Text("로그인") }
                    TextButton(enabled = !busy, onClick = { launchAuth {
                        val result = auth.signUp(email, password)
                        session = result.session
                        onSessionChanged()
                        message = result.message
                    } }, modifier = Modifier.weight(1f)) { Text("회원가입") }
                }
            }
            message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}
