package ca.uqac.mobile.projetfilrouge.ui.screen

import androidx.annotation.PluralsRes
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import ca.uqac.mobile.projetfilrouge.R
import androidx.compose.ui.res.stringResource
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import ca.uqac.mobile.projetfilrouge.data.Friend
import ca.uqac.mobile.projetfilrouge.ui.components.ScreenLayout
import ca.uqac.mobile.projetfilrouge.ui.components.SmallSwitch
import ca.uqac.mobile.projetfilrouge.ui.theme.ProjetFilRougeTheme
import java.util.Calendar
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
    val memberProfiles by viewModel.memberProfiles.collectAsStateWithLifecycle()

    AlarmsContent(
        alarms = alarms,
        memberProfiles = memberProfiles,
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
    memberProfiles: Map<String, Friend?>,
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
    // L'alarme activée qui sonnera en premier, avec l'instant où elle sonnera
    val nextAlarm = alarms
        .filter { it.isEnabledFor(myUid) }
        .map { it to AlarmScheduler.nextOccurrence(it.hour, it.minute, it.days, now) }
        .minByOrNull { it.second }

    ScreenLayout {
        Spacer(Modifier.height(24.dp))
        NextAlarmCard(nextAlarm, hasAlarms = alarms.isNotEmpty(), nowMillis = now)
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.alarms_list_title),
                modifier = Modifier.weight(1f).padding(start = 4.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            IconButton(onClick = onAdd) {
                Icon(Icons.Outlined.AddCircleOutline, contentDescription = stringResource(R.string.alarms_add))
            }
        }
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        if (alarms.isEmpty()) {
            Text(
                stringResource(R.string.alarms_empty),
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
                    members = alarm.members.take(5).map { memberProfiles[it] },
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
    members: List<Friend?>,
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
                MemberAvatars(members)
            }
        }
    }
}

@Composable
private fun daysLabel(days: List<Int>): String {
    val dayNames = stringArrayResource(R.array.day_names_short)
    return when {
        days.isEmpty() || days.size == 7 -> stringResource(R.string.alarms_every_day)
        days.sorted() == listOf(1, 2, 3, 4, 5) -> stringResource(R.string.alarms_weekdays)
        days.sorted() == listOf(0, 6) -> stringResource(R.string.alarms_weekends)
        else -> days.sorted().joinToString(", ") { dayNames[it] }
    }
}

@Composable
private fun NextAlarmCard(next: Pair<SharedAlarm, Long>?, hasAlarms: Boolean, nowMillis: Long) {
    AppCard(contentPadding = PaddingValues(24.dp)) {
        Text(
            stringResource(R.string.alarms_next),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        if (next == null) {
            Text(
                stringResource(R.string.alarms_none_scheduled),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            // Sans alarme du tout, le message sous la carte invite déjà à en créer une
            if (hasAlarms) {
                Text(
                    stringResource(R.string.alarms_all_disabled),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@AppCard
        }
        val (alarm, atMillis) = next
        Row {
            Text(
                alarm.timeText,
                modifier = Modifier.alignByBaseline(),
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                dayLabel(atMillis, nowMillis),
                modifier = Modifier.alignByBaseline(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            untilText(atMillis, nowMillis),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        if (alarm.name.isNotBlank()) {
            Text(
                alarm.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// "Aujourd'hui", "Demain", sinon le nom du jour
@Composable
private fun dayLabel(targetMillis: Long, nowMillis: Long): String {
    fun midnight(millis: Long) = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val target = midnight(targetMillis)
    // Arrondi : un changement d'heure (été / hiver) donne une journée de 23 h ou 25 h
    val dayDiff = Math.round((target.timeInMillis - midnight(nowMillis).timeInMillis) / 86_400_000.0)
    return when (dayDiff) {
        0L -> stringResource(R.string.alarms_today)
        1L -> stringResource(R.string.alarms_tomorrow)
        else -> stringArrayResource(R.array.day_names)[target.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY]
    }
}

// "dans 2 heures et 5 minutes", "dans 3 jours et 1 heure", "dans 10 minutes" : on n'affiche pas les unités à 0
@Composable
private fun untilText(targetMillis: Long, nowMillis: Long): String {
    val totalMinutes = (targetMillis - nowMillis + 59_999) / 60_000
    val days = totalMinutes / (24 * 60)
    val hours = (totalMinutes / 60) % 24
    val minutes = totalMinutes % 60
    // pluralStringResource choisit "1 hour" / "2 hours" (ou "1 heure" / "2 heures") selon la langue
    @Composable
    fun plural(@PluralsRes id: Int, value: Long) = pluralStringResource(id, value.toInt(), value.toInt())
    val parts = buildList {
        if (days > 0) add(plural(R.plurals.duration_days, days))
        if (hours > 0) add(plural(R.plurals.duration_hours, hours))
        if (days == 0L && minutes > 0) add(plural(R.plurals.duration_minutes, minutes))
    }
    return when (parts.size) {
        1 -> stringResource(R.string.alarms_in_one, parts[0])
        else -> stringResource(R.string.alarms_in_two, parts[0], parts[1])
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
            memberProfiles = emptyMap(),
            myUid = "me",
            error = null,
            onAdd = {},
            onOpen = {},
            onEnabledChange = { _, _ -> },
        )
    }
}
