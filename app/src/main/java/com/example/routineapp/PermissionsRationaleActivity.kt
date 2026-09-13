package com.example.routineapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.routineapp.ui.theme.RoutineAppTheme

class PermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RoutineAppTheme {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("운동 데이터 사용 안내", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "RoutineApp은 운동 루틴 자동 체크를 위해 Health Connect의 운동 세션 데이터만 읽습니다. " +
                            "운동 데이터는 루틴 완료 여부를 판단하는 데 사용되며 다른 목적으로 전송하지 않습니다.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
        }
    }
}
