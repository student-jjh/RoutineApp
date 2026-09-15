package com.example.routineapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private data class GuidePage(val icon: ImageVector, val title: String, val body: String, val hint: String)

private val guidePages = listOf(
    GuidePage(Icons.Default.AddCircle, "나에게 맞는 루틴부터", "홈의 + 또는 내 루틴의 ‘새 루틴 만들기’로 시작하세요. 분류와 반복할 요일을 선택할 수 있어요.", "루틴을 수정하려면 내 루틴에서 카드를 누르세요. 삭제는 카드 오른쪽 메뉴에 있어요."),
    GuidePage(Icons.Default.CheckCircle, "오늘은 가볍게 체크", "오늘 할 루틴만 홈에 모여요. 카드 전체를 누르면 완료, 다시 누르면 취소할 수 있어요.", "홈에서 카드를 길게 누르면 수정 메뉴가 나와요. 하루 75% 이상 완료하면 달성일로 인정돼요."),
    GuidePage(Icons.Default.FitnessCenter, "운동은 기록으로 연결", "운동 탭에서 Health Connect를 연결하면 조건에 맞는 오늘 운동이 루틴에 반영돼요. 유산소 로그를 눌러 거리·페이스 등도 살펴보세요.", "삼성 헬스 등 원본 앱에서도 Health Connect 데이터 공유가 필요해요. 지표는 원본 앱이 제공하고 권한을 허용한 경우에 표시돼요. 아래로 당기면 새로고침돼요."),
    GuidePage(Icons.Default.EditNote, "세트와 휴식을 한곳에서", "근력 루틴의 기록 아이콘이나 운동 탭에서 근력 로그를 여세요. + 기록으로 종목을 고르고 세트마다 중량과 횟수를 입력하세요.", "맨몸 운동은 중량을 비워두세요. 작성·수정 화면의 휴식 타이머로 쉬는 시간을 확인하고, 운동별·부위별로 지난 기록을 찾아볼 수 있어요."),
    GuidePage(Icons.Default.CalendarMonth, "지난 하루도 돌아보세요", "기록 탭에서 달력이나 날짜 이동 버튼으로 지난 루틴을 확인하세요. 늦게 체크했어도 해당 날짜의 루틴을 눌러 완료 여부를 수정할 수 있어요.", "달력 색으로 하루 달성률을 보고, 루틴별 달성률은 펼쳐서 확인하세요. 이 안내는 내 루틴 → 사용 가이드에서 다시 볼 수 있어요.")
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    var page by rememberSaveable { mutableStateOf(0) }
    val item = guidePages[page]
    Dialog(onDismissRequest = onFinish, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("ROUTIVE · 사용 가이드", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = onFinish) { Text("닫기") }
                }
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primary) {
                        Icon(item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(24.dp).size(48.dp))
                    }
                    Text(item.title, style = MaterialTheme.typography.headlineLarge)
                    Text(item.body, style = MaterialTheme.typography.bodyLarge)
                    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
                        Text(item.hint, modifier = Modifier.padding(20.dp), style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text("${page + 1} / ${guidePages.size}", style = MaterialTheme.typography.labelMedium)
                LinearProgressIndicator(progress = { (page + 1f) / guidePages.size }, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (page > 0) OutlinedButton(onClick = { page-- }, modifier = Modifier.height(52.dp)) { Text("이전") }
                    Button(onClick = { if (page == guidePages.lastIndex) onFinish() else page++ }, modifier = Modifier.weight(1f).height(52.dp)) {
                        Text(if (page == guidePages.lastIndex) "시작하기" else "다음")
                    }
                }
            }
        }
    }
}
