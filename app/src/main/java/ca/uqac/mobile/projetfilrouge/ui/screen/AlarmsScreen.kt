package ca.uqac.mobile.projetfilrouge.ui.screen

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ca.uqac.mobile.projetfilrouge.AlarmScheduler
import ca.uqac.mobile.projetfilrouge.data.SharedAlarm
import ca.uqac.mobile.projetfilrouge.ui.components.AppCard
import ca.uqac.mobile.projetfilrouge.ui.components.CardShape
import ca.uqac.mobile.projetfilrouge.ui.components.MemberAvatars
import ca.uqac.mobile.projetfilrouge.ui.components.ScreenLayout
import ca.uqac.mobile.projetfilrouge.ui.components.ScreenTitle
import ca.uqac.mobile.projetfilrouge.ui.components.SmallSwitch
import ca.uqac.mobile.projetfilrouge.ui.theme.ProjetFilRougeTheme
import ca.uqac.mobile.projetfilrouge.viewModel.SharedAlarmsViewModel
import kotlinx.coroutines.delay

@Composable
fun AlarmsScreen(onAlarmEdit: (String?) -> Unit) {
    // Android 13+ : sans cette permission, la notification de l'alarme ne s'affiche pas.
    // Demandée une seule fois, à l'ouverture de l'écran.
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    val viewModel: SharedAlarmsViewModel = viewModel()
    val alarms by viewModel.alarms.collectAsStateWithLifecycle()
    val uid by viewModel.uid.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    AlarmsContent(
        alarms = alarms,
        myUid = uid.orEmpty(),
        error = error,
        onAdd = { onAlarmEdit(null) },
        onOpen = { onAlarmEdit(it.id) },
        onEnabledChange = viewModel::setEnabled,
    )
}

@Composable
private fun AlarmsContent(
    alarms: List<SharedAlarm>,
    myUid: String,
    error: String?,
    onAdd: () -> Unit,
    onOpen: (SharedAlarm) -> Unit,
    onEnabledChange: (SharedAlarm, Boolean) -> Unit,
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = System.currentTimeMillis()
        }
    }
    val nextAlarm = alarms
        .filter { it.isEnabledFor(myUid) }
        .minOfOrNull { AlarmScheduler.nextOccurrence(it.hour, it.minute, it.days, now) }

    ScreenLayout {
        Spacer(Modifier.height(24.dp))
        ScreenTitle(
            if (nextAlarm != null) untilText(nextAlarm, now) else "No alarm\nscheduled",
            Modifier.fillMaxWidth(),
        )
        IconButton(onClick = onAdd, modifier = Modifier.align(Alignment.End)) {
            Icon(Icons.Outlined.AddCircleOutline, contentDescription = "Add an alarm")
        }
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        if (alarms.isEmpty()) {
            Text(
                "No alarms yet. Tap + to create one.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            items(alarms, key = { it.id }) { alarm ->
                AlarmCard(
                    alarm = alarm,
                    enabled = alarm.isEnabledFor(myUid),
                    onClick = { onOpen(alarm) },
                    onEnabledChange = { onEnabledChange(alarm, it) },
                )
            }
        }
    }
}

@Composable
private fun AlarmCard(
    alarm: SharedAlarm,
    enabled: Boolean,
    onClick: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
) {
    AppCard(
        modifier = Modifier
            .clip(CardShape)
            .clickable(onClick = onClick),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                Modifier
                    .weight(1f)
                    .alpha(if (enabled) 1f else 0.5f)
            ) {
                Text(alarm.timeText, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Text(
                    listOf(alarm.name, daysLabel(alarm.days)).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                SmallSwitch(enabled, onCheckedChange = onEnabledChange)
                MemberAvatars(alarm.members.size.coerceAtMost(5))
            }
        }
    }
}

private val DayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

private fun daysLabel(days: List<Int>): String = when {
    days.isEmpty() || days.size == 7 -> "Every day"
    days.sorted() == listOf(1, 2, 3, 4, 5) -> "Weekdays"
    days.sorted() == listOf(0, 6) -> "Weekends"
    else -> days.sorted().joinToString(", ") { DayNames[it] }
}

private fun untilText(targetMillis: Long, nowMillis: Long): String {
    val totalMinutes = (targetMillis - nowMillis + 59_999) / 60_000
    val days = totalMinutes / (24 * 60)
    val hours = (totalMinutes / 60) % 24
    val minutes = totalMinutes % 60
    fun plural(value: Long, word: String) = if (value == 1L) "1 $word" else "$value ${word}s"
    return if (days > 0) {
        "Alarm in ${plural(days, "day")}\nand ${plural(hours, "hour")}"
    } else {
        "Alarm in ${plural(hours, "hour")}\nand ${plural(minutes, "minute")}"
    }
}

@Preview(showSystemUi = true)
@Composable
private fun AlarmsScreenPreview() {
    ProjetFilRougeTheme {
        AlarmsContent(
            alarms = listOf(
                SharedAlarm(id = "1", name = "Gym", hour = 7, minute = 0, days = listOf(1, 3, 5), members = listOf("me")),
                SharedAlarm(id = "2", hour = 8, minute = 30, members = listOf("me", "a", "b")),
            ),
            myUid = "me",
            error = null,
            onAdd = {},
            onOpen = {},
            onEnabledChange = { _, _ -> },
        )
    }
}
