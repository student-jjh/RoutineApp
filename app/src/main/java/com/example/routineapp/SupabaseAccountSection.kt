package com.example.routineapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.routineapp.data.SupabaseAuth
import com.example.routineapp.data.SupabaseConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun SupabaseAccountSection(config: SupabaseConfig) {
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

    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Text("계정 연결", style = MaterialTheme.typography.titleSmall)
        if (session != null) {
            Text("${session!!.user.email ?: "이메일 계정"}으로 로그인됨", style = MaterialTheme.typography.bodyMedium)
            Text("로그인만 연결된 상태예요. 클라우드 백업은 다음 단계에서 추가해요.", style = MaterialTheme.typography.bodySmall)
            TextButton(enabled = !busy, onClick = {
                busy = true
                scope.launch {
                    try {
                        auth.signOut()
                        session = null
                        message = "로그아웃했어요. 로컬 기록은 그대로 유지돼요."
                    } finally { busy = false }
                }
            }) { Text("로그아웃") }
        } else {
            OutlinedTextField(value = email, onValueChange = { email = it }, enabled = !busy,
                singleLine = true, label = { Text("이메일") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = password, onValueChange = { password = it }, enabled = !busy,
                singleLine = true, visualTransformation = PasswordVisualTransformation(),
                label = { Text("비밀번호 (6자 이상)") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = !busy, onClick = { launchAuth {
                    val result = auth.signIn(email, password)
                    session = result.session
                    message = result.message
                } }) {
                    Text("로그인")
                }
                TextButton(enabled = !busy, onClick = { launchAuth {
                    val result = auth.signUp(email, password)
                    session = result.session
                    message = result.message
                } }) {
                    Text("회원가입")
                }
            }
        }
        message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
}
