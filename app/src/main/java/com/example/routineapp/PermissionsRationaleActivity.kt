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
                        "루티브는 운동 루틴 자동 체크와 유산소 통계를 위해 Health Connect의 운동 세션과 거리 데이터를 읽습니다. " +
                            "데이터는 거리, 운동 시간과 페이스를 분석하는 데 사용되며 외부로 전송하지 않습니다.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
        }
    }
}
